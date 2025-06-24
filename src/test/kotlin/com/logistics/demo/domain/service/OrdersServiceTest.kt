package com.logistics.demo.domain.service

import com.logistics.demo.domain.Operation
import com.logistics.demo.repository.OperationsRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrdersServiceTest {

    private lateinit var repository: OperationsRepository
    private lateinit var service: OrdersService
    private val clientId = "CLI-123"

    @BeforeAll
    fun setUp() {
        repository = mock(OperationsRepository::class.java)
        service = OrdersService(repository)
    }

    @Test
    fun `getAllByClientId returns orders when present`() {
        val order1 = Operation.Order("ORD-1", null)
        val order2 = Operation.Order("ORD-2", null)
        val op1 = mock(Operation::class.java)
        val op2 = mock(Operation::class.java)
        `when`(op1.orders).thenReturn(listOf(order1))
        `when`(op2.orders).thenReturn(listOf(order2))
        `when`(repository.findAllByClientId(clientId)).thenReturn(listOf(op1, op2))

        val result = service.getAllByClientId(clientId)
        Assertions.assertEquals(listOf(order1, order2), result)
    }

    @Test
    fun `getAllByClientId returns empty list when no operations`() {
        `when`(repository.findAllByClientId(clientId)).thenReturn(emptyList())

        val result = service.getAllByClientId(clientId)
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllByClientId skips null orders`() {
        val op1 = mock(Operation::class.java)
        `when`(op1.orders).thenReturn(null)
        `when`(repository.findAllByClientId(clientId)).thenReturn(listOf(op1))

        val result = service.getAllByClientId(clientId)
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `getContainersByPurchaseId returns containers from repository`() {
        val container = Operation.Container("CONT-1")
        `when`(repository.getContainersByPurchaseId(clientId, "PUR-1")).thenReturn(listOf(container))

        val result = service.getContainersByPurchaseId(clientId, "PUR-1")
        Assertions.assertEquals(listOf(container), result)
    }

    @Test
    fun `getContainersByPurchaseId returns empty list when no containers`() {
        `when`(repository.getContainersByPurchaseId(clientId, "PUR-1")).thenReturn(emptyList())

        val result = service.getContainersByPurchaseId(clientId, "PUR-1")
        Assertions.assertTrue(result.isEmpty())
    }
}