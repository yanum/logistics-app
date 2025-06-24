package com.logistics.demo.controller

import TokenUtils
import com.logistics.demo.controller.OrderController
import com.logistics.demo.domain.Operation
import com.logistics.demo.domain.service.OrdersService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class OrderControllerTest {
    private lateinit var ordersService: OrdersService
    private lateinit var controller: OrderController
    private val validToken = "Bearer valid-token"
    private val validClientId = "CLI-123"
    private val purchaseId = "PUR-123"

    @BeforeEach
    fun setUp() {
        ordersService = mockk(relaxed = true)
        controller = OrderController(ordersService)
        mockkObject(TokenUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getOrders returns a success response with valid token`() {
        val orders = listOf(
            Operation.Order(purchaseId, listOf(Operation.Invoice("inv1"))),
            Operation.Order("PUR-456", listOf(Operation.Invoice("inv2")))
        )
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { ordersService.getAllByClientId(validClientId) } returns orders

        val response = controller.getOrders(validToken)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.data is List<*>)
        assertEquals(orders, response.body!!.data)

        verify(exactly = 1) { ordersService.getAllByClientId(validClientId) }
    }

    @Test
    fun `getOrders returns bad request with empty token`() {
        every { TokenUtils.extractClientIdFromJwt("") } returns null

        val response = controller.getOrders("")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid or missing Authorization header"))

        verify(exactly = 0) { ordersService.getAllByClientId(any()) }
    }

    @Test
    fun `getOrders handles service exception successfully`() {
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { ordersService.getAllByClientId(validClientId) } throws RuntimeException("Service error")

        val response = controller.getOrders(validToken)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Service error"))

        verify(exactly = 1) { ordersService.getAllByClientId(validClientId) }
    }

    @Test
    fun `getContainersByPurchaseId returns success with valid inputs`() {
        val containers = listOf(
            Operation.Container("CONT-123"),
            Operation.Container("CONT-456")
        )
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { ordersService.getContainersByPurchaseId(validClientId, purchaseId) } returns containers

        val response = controller.getContainersByPurchaseId(validToken, purchaseId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.data is List<*>)
        assertEquals(containers, response.body!!.data)

        verify(exactly = 1) { ordersService.getContainersByPurchaseId(validClientId, purchaseId) }
    }

    @Test
    fun `getContainersByPurchaseId returns bad request with empty token`() {
        every { TokenUtils.extractClientIdFromJwt("") } returns null

        val response = controller.getContainersByPurchaseId("", purchaseId)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid or missing Authorization header"))

        verify(exactly = 0) { ordersService.getContainersByPurchaseId(any(), any()) }
    }

    @Test
    fun `getContainersByPurchaseId handles service exception successfully`() {
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { ordersService.getContainersByPurchaseId(validClientId, purchaseId) } throws RuntimeException("Service error")

        val response = controller.getContainersByPurchaseId(validToken, purchaseId)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Service error"))

        verify(exactly = 1) { ordersService.getContainersByPurchaseId(validClientId, purchaseId) }
    }

    @Test
    fun `getContainersByPurchaseId handles invalid purchase id`() {
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { ordersService.getContainersByPurchaseId(validClientId, "invalid-id") } throws IllegalArgumentException("Purchase not found")

        val response = controller.getContainersByPurchaseId(validToken, "invalid-id")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Purchase not found"))

        verify(exactly = 1) { ordersService.getContainersByPurchaseId(validClientId, "invalid-id") }
    }
}