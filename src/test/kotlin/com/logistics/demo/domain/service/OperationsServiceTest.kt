package com.logistics.demo.domain.service

import com.logistics.demo.controller.dto.EmailRequest
import com.logistics.demo.controller.dto.EmailRequest.*
import com.logistics.demo.domain.Item
import com.logistics.demo.domain.Operation
import com.logistics.demo.repository.ItemsRepository
import com.logistics.demo.repository.OperationsRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationsServiceTest {
    private lateinit var operationsRepository: OperationsRepository
    private lateinit var itemsRepository: ItemsRepository
    private lateinit var service: OperationsService
    private val clientId = "CLI-123"

    @BeforeEach
    fun setUp() {
        operationsRepository = mockk(relaxed = true)
        itemsRepository = mockk(relaxed = true)
        service = OperationsService(operationsRepository, itemsRepository)
    }

    @Nested
    inner class SaveNewOperation {
        @Test
        fun `creates new operation with all fields`() {
            val request = EmailRequest(
                booking = "BKG-1",
                containers = listOf(ContainerDto("CONT-1")),
                orders = listOf(
                    OrderDto(
                        purchase = "ORD-1",
                        invoices = listOf(InvoiceDto("INV-1"))
                    )
                )
            )
            every { operationsRepository.findByClientIdAndBookingId(clientId, "BKG-1") } returns null
            every { itemsRepository.getById(any()) } returns null

            val result = service.save(request, clientId)

            assertEquals(clientId, result.clientId)
            assertEquals("BKG-1", result.booking)
            assertEquals("CONT-1", result.containers?.first()?.container)
            assertEquals("ORD-1", result.orders?.first()?.purchase)
            assertEquals("INV-1", result.orders?.first()?.invoices?.first()?.invoice)

            verify {
                operationsRepository.save(any())
                itemsRepository.save(match { it.id == "CONT-1" })
                itemsRepository.save(match { it.id == "ORD-1" })
                itemsRepository.save(match { it.id == "INV-1" })
            }
        }

        @Test
        fun `creates new operation with only containers`() {
            val request = EmailRequest(
                booking = "BKG-1",
                containers = listOf(ContainerDto("CONT-1")),
                orders = null
            )
            every { operationsRepository.findByClientIdAndBookingId(clientId, "BKG-1") } returns null
            every { itemsRepository.getById(any()) } returns null

            val result = service.save(request, clientId)

            assertEquals("CONT-1", result.containers?.first()?.container)
            assertTrue { result.orders.isNullOrEmpty() }

            verify {
                operationsRepository.save(any())
                itemsRepository.save(match { it.id == "CONT-1" })
            }
        }

        @Test
        fun `creates new operation with only orders`() {
            val request = EmailRequest(
                booking = "BKG-1",
                containers = null,
                orders = listOf(OrderDto("ORD-1", null))
            )
            every { operationsRepository.findByClientIdAndBookingId(clientId, "BKG-1") } returns null
            every { itemsRepository.getById(any()) } returns null

            val result = service.save(request, clientId)

            assertTrue { result.containers.isNullOrEmpty() }
            assertEquals("ORD-1", result.orders?.first()?.purchase)

            verify {
                operationsRepository.save(any())
                itemsRepository.save(match { it.id == "ORD-1" })
            }
        }
    }

    @Nested
    inner class UpdateExistingOperation {
        @Test
        fun `updates existing operation with new items`() {
            val existing = Operation(
                clientId = clientId,
                booking = "BKG-1",
                containers = listOf(Operation.Container("CONT-1")),
                orders = listOf(Operation.Order("ORD-1", null))
            )
            val request = EmailRequest(
                booking = "BKG-1",
                containers = listOf(ContainerDto("CONT-2")),
                orders = listOf(
                    OrderDto(
                        purchase = "ORD-2",
                        invoices = listOf(InvoiceDto("INV-1"))
                    )
                )
            )
            every { operationsRepository.findByClientIdAndBookingId(clientId, "BKG-1") } returns existing
            every { itemsRepository.getById(any()) } returns null

            val result = service.save(request, clientId)

            assertEquals(2, result.containers?.size)
            assertEquals(2, result.orders?.size)
            assertTrue { result.containers?.any { it.container == "CONT-1" } == true }
            assertTrue { result.containers?.any { it.container == "CONT-2" } == true }
            assertTrue { result.orders?.any { it.purchase == "ORD-1" } == true }
            assertTrue { result.orders?.any { it.purchase == "ORD-2" } == true }

            verify {
                operationsRepository.save(any())
                itemsRepository.save(match { it.id == "CONT-2" })
                itemsRepository.save(match { it.id == "ORD-2" })
                itemsRepository.save(match { it.id == "INV-1" })
            }
        }
    }

    @Nested
    inner class ValidationTests {
        @Test
        fun `throws exception for duplicate items in request`() {
            val request = EmailRequest(
                booking = "BKG-1",
                containers = listOf(
                    ContainerDto("CONT-1"),
                    ContainerDto("CONT-1")
                ),
                orders = null
            )
            every { operationsRepository.findByClientIdAndBookingId(any(), any()) } returns null

            assertThrows<IllegalArgumentException>("Duplicate items found in the request") {
                service.save(request, clientId)
            }
        }

        @Test
        fun `throws exception when item exists with same type`() {
            val request = EmailRequest(
                booking = "BKG-1",
                containers = listOf(ContainerDto("CONT-1")),
                orders = null
            )
            every { operationsRepository.findByClientIdAndBookingId(any(), any()) } returns null
            every { itemsRepository.getById("CONT-1") } returns Item("CONT-1", "CONTAINER")

            assertThrows<IllegalArgumentException>("Some items already exist with the same type") {
                service.save(request, clientId)
            }
        }
    }
}