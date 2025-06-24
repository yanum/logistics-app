import com.logistics.demo.controller.ContainerController
import com.logistics.demo.domain.Operation
import com.logistics.demo.domain.service.ContainersService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContainerControllerTest {
    private lateinit var containersService: ContainersService
    private lateinit var controller: ContainerController
    private val validToken = "Bearer valid-token"
    private val validClientId = "CLI-123"

    @BeforeEach
    fun setUp() {
        containersService = mockk(relaxed = true)
        controller = ContainerController(containersService)
        mockkObject(TokenUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getContainers returns success with valid token`() {
        val containers = listOf(
            Operation.Container("CONT-1"),
            Operation.Container("CONT-2")
        )
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { containersService.getAllByClientId(validClientId) } returns containers

        val response = controller.getContainers(validToken)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.data is List<*>)
        assertEquals(containers, response.body!!.data)
    }

    @Test
    fun `getContainers returns bad request with empty token`() {
        every { TokenUtils.extractClientIdFromJwt("") } returns null

        val response = controller.getContainers("")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid or missing Authorization header"))
    }

    @Test
    fun `getContainers returns bad request with invalid token`() {
        every { TokenUtils.extractClientIdFromJwt("invalid-token") } returns null

        val response = controller.getContainers("invalid-token")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
    }

    @Test
    fun `getContainers handles service exception gracefully`() {
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { containersService.getAllByClientId(validClientId) } throws RuntimeException("Service error")

        val response = controller.getContainers(validToken)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Service error"))
    }

    @Test
    fun `getOrdersByContainerId returns success with valid inputs`() {
        val orders = listOf(
            Operation.Order(purchase = "PUR-123", invoices = listOf(Operation.Invoice("inv1"))),
            Operation.Order(purchase = "PUR-456", invoices = listOf(Operation.Invoice("inv2")))
        )
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { containersService.getOrdersByContainerId(validClientId, "CONT-123") } returns orders

        val response = controller.getOrdersByContainerId(validToken, "CONT-123")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.data is List<*>)
        assertEquals(orders, response.body!!.data)
    }

    @Test
    fun `getOrdersByContainerId returns bad request with empty token`() {
        every { TokenUtils.extractClientIdFromJwt("") } returns null

        val response = controller.getOrdersByContainerId("", "CONT-123")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Invalid or missing Authorization header"))
    }

    @Test
    fun `getOrdersByContainerId handles service exception gracefully`() {
        every { TokenUtils.extractClientIdFromJwt(validToken) } returns validClientId
        every { containersService.getOrdersByContainerId(validClientId, "CONT-123") } throws RuntimeException("Service error")

        val response = controller.getOrdersByContainerId(validToken, "CONT-123")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertTrue(response.body!!.error!!.contains("Service error"))
    }
}