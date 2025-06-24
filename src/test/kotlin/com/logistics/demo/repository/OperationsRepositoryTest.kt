package com.logistics.demo.repository

import com.logistics.demo.domain.Operation
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperationsRepositoryTest {
    private lateinit var dynamoDbClient: DynamoDbClient
    private lateinit var cacheRepository: OperationsCacheRepository
    private lateinit var repository: OperationsRepository
    private lateinit var logger: Logger
    private lateinit var circuitBreaker: CircuitBreaker

    private val testOperation = Operation(
        clientId = "CLI-123",
        booking = "BKG-1",
        containers = listOf(Operation.Container("CONT-1")),
        orders = listOf(Operation.Order("ORD-1"))
    )

    @BeforeEach
    fun setUp() {
        dynamoDbClient = mockk(relaxed = true)
        cacheRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("testBreaker")

        repository = OperationsRepository(dynamoDbClient, cacheRepository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    inner class SaveOperationTests {
        @Test
        fun `save operation successfully`() {
            val slot = slot<PutItemRequest>()
            every { dynamoDbClient.putItem(capture(slot)) } returns PutItemResponse.builder().build()

            repository.save(testOperation)

            verify(exactly = 1) {
                dynamoDbClient.putItem(any<PutItemRequest>())
            }
            with(slot.captured) {
                assertEquals("EmailRequests", tableName())
                assertTrue { item().containsKey("id") }
                assertEquals("CLI-123", item()["client_id"]?.s())
            }
        }

        @Test
        fun `save operation should fail`() {
            every { dynamoDbClient.putItem(any<PutItemRequest>()) } throws
                ProvisionedThroughputExceededException.builder().build() andThen
                PutItemResponse.builder().build()

            assertThrows<RepositoryException>{ repository.save(testOperation)}

            verify(exactly = 1) { dynamoDbClient.putItem(any<PutItemRequest>()) }
        }

    }

    @Nested
    inner class FindByClientIdTests {
        @Test
        fun `findAllByClientId returns operations successfully`() {
            val response = QueryResponse.builder()
                .items(listOf(
                    mapOf(
                        "client_id" to AttributeValue.fromS("CLI-123"),
                        "booking" to AttributeValue.fromS("BKG-1"),
                        "containers" to AttributeValue.fromS("CONT-1"),
                        "orders" to AttributeValue.fromS("ORD-1")
                    )
                ))
                .build()
            every { dynamoDbClient.query(any<QueryRequest>()) } returns response

            val result = repository.findAllByClientId("CLI-123")

            assertEquals(1, result.size)
            verify(exactly = 1) {
                dynamoDbClient.query(any<QueryRequest>())
                cacheRepository.cacheAll(any())
            }
        }

        @Test
        fun `findAllByClientId falls back to cache on failure`() {
            val expectedResult = listOf(testOperation)
            every { dynamoDbClient.query(any<QueryRequest>()) } throws RuntimeException("Network error")
            every { cacheRepository.findAllByClientId("CLI-123") } returns expectedResult

            val result =  repository.findAllByClientId("CLI-123")

            assertEquals(expectedResult, result)
            verify(exactly = 1) { cacheRepository.findAllByClientId("CLI-123") }
        }
    }

    @Nested
    inner class FindByClientIdAndBookingIdTests {
        @Test
        fun `findByClientIdAndBookingId returns operation when found`() {
            val response = GetItemResponse.builder()
                .item(mapOf(
                    "client_id" to AttributeValue.fromS("CLI-123"),
                    "booking" to AttributeValue.fromS("BKG-1")
                ))
                .build()
            every { dynamoDbClient.getItem(any<GetItemRequest>()) } returns response

            val result = repository.findByClientIdAndBookingId("CLI-123", "BKG-1")

            assertTrue { result != null }
            verify(exactly = 1) {
                dynamoDbClient.getItem(any<GetItemRequest>())
                cacheRepository.cache(any())
            }
        }

        @Test
        fun `findByClientIdAndBookingId returns null when not found`() {
            every { dynamoDbClient.getItem(any<GetItemRequest>()) } returns GetItemResponse.builder().build()

            val result = repository.findByClientIdAndBookingId("CLI-123", "BKG-1")

            assertNull(result)
            verify(exactly = 1) { dynamoDbClient.getItem(any<GetItemRequest>()) }
            verify(exactly = 0) { cacheRepository.cache(any()) }
        }

        @Test
        fun `findByClientIdAndBookingId uses cache on circuit breaker open`() {
            val expectedResult = testOperation
            every { dynamoDbClient.getItem(any<GetItemRequest>()) } throws TimeoutException()
            every { cacheRepository.findByClientIdAndBookingId(any(), any()) } returns expectedResult

            val result = repository.findByClientIdAndBookingId("CLI-123", "BKG-1")

            assertEquals(expectedResult, result)
            verify(exactly = 1) { cacheRepository.findByClientIdAndBookingId("CLI-123", "BKG-1") }
        }
    }

    @Test
    fun `getContainersByPurchaseId successfully`() {
        val response = QueryResponse.builder()
            .items(listOf(
                mapOf("containers" to AttributeValue.fromS("CONT-1, CONT-2"))
            ))
            .build()
        every { dynamoDbClient.query(any<QueryRequest>()) } returns response

        val result = repository.getContainersByPurchaseId("CLI-123", "ORD-1")

        assertEquals(2, result.size)
        verify(exactly = 1) { dynamoDbClient.query(any<QueryRequest>()) }
    }

    @Test
    fun `getOrdersByContainerId successfully`() {
        every { dynamoDbClient.query(any<QueryRequest>()) } returns QueryResponse.builder()
            .items(listOf(
                mapOf(
                    "orders" to AttributeValue.fromS("ORD-1, ORD-2"),
                    "containers" to AttributeValue.fromS("CONT-1")
                )
            ))
            .build()

        val result = repository.getOrdersByContainerId("CLI-123", "CONT-1")

        assertEquals(2, result.size)
    }
}