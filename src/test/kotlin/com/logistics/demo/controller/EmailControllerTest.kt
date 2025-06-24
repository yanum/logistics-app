package com.logistics.demo.controller

import TokenUtils
import com.logistics.demo.controller.EmailController
import com.logistics.demo.controller.dto.EmailRequest
import com.logistics.demo.domain.Operation
import com.logistics.demo.domain.Operation.Container
import com.logistics.demo.domain.Operation.Order
import com.logistics.demo.domain.service.OperationsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.mockk.confirmVerified
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EmailControllerTest {
    private lateinit var operationsService: OperationsService
    private lateinit var controller: EmailController
    private val validToken = "Bearer valid-token"
    private val validClientId = "client123"

    @BeforeEach
    fun setUp() {
        operationsService = mockk()
        controller = EmailController(operationsService)
        mockkObject(TokenUtils)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(operationsService)
        unmockkAll()
    }

    @Test
    fun `sendEmail returns success with complete request`() {
        val emailRequest = EmailRequest(
            booking = "BOOK-123",
            containers = listOf(EmailRequest.ContainerDto("CONT-123")),
            orders = listOf(
                EmailRequest.OrderDto(
                    purchase = "PUR-123",
                    invoices = listOf(EmailRequest.InvoiceDto("INV-123"))
                )
            )
        )
        val savedOperation = Operation(
            "CLI-123", "BOOK-123", listOf(Container("CONT-123")),
            listOf(Order("PUR-123", listOf(Operation.Invoice("INV-123")))))

        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { operationsService.save(emailRequest, validClientId) } returns savedOperation

        val response = controller.sendEmail(emailRequest, validToken)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(savedOperation, response.body!!.data)

        verify(exactly = 1) { operationsService.save(emailRequest, validClientId) }
    }

    @Test
    fun `sendEmail returns success with minimal request`() {
        val emailRequest = EmailRequest(booking = "BOOK-123")
        val savedOperation = Operation("CLI-123", "BOOK-123", emptyList(), emptyList())

        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { operationsService.save(emailRequest, validClientId) } returns savedOperation

        val response = controller.sendEmail(emailRequest, validToken)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(savedOperation, response.body!!.data)

        verify(exactly = 1) { operationsService.save(emailRequest, validClientId) }
    }

    @Test
    fun `sendEmail returns bad request with empty authorization header`() {
        val emailRequest = EmailRequest(booking = "BOOK-123")
        every { TokenUtils.extractClientIdFromJwt("") } returns null

        val response = controller.sendEmail(emailRequest, "")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid or missing Authorization header"))

        verify(exactly = 0) { operationsService.save(any(), any()) }
    }

    @Test
    fun `sendEmail returns bad request with invalid token`() {
        val emailRequest = EmailRequest(booking = "BOOK-123")
        every { TokenUtils.extractClientIdFromJwt("invalid-token") } returns null

        val response = controller.sendEmail(emailRequest, "invalid-token")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid or missing Authorization header"))

        verify(exactly = 0) { operationsService.save(any(), any()) }
    }

    @Test
    fun `sendEmail handles service validation exception`() {
        val emailRequest = EmailRequest(booking = "BOOK-123")
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every {
            operationsService.save(
                emailRequest,
                validClientId
            )
        } throws IllegalArgumentException("Invalid booking number")

        val response = controller.sendEmail(emailRequest, validToken)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid booking number"))

        verify(exactly = 1) { operationsService.save(emailRequest, validClientId) }
    }

    @Test
    fun `sendEmail handles unexpected service error`() {
        val emailRequest = EmailRequest(booking = "BOOK-123")
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { operationsService.save(emailRequest, validClientId) } throws RuntimeException("Unexpected error")

        val response = controller.sendEmail(emailRequest, validToken)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Unexpected error"))

        verify(exactly = 1) { operationsService.save(emailRequest, validClientId) }
    }
}