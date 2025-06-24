package com.logistics.demo.domain.service

import com.logistics.demo.domain.Operation
import com.logistics.demo.repository.OperationsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainersServiceTest {

    private lateinit var repository: OperationsRepository
    private lateinit var service: ContainersService
    private val clientId = "CLI-123"
    private val containerId = "CONT-456"

    @BeforeAll
    fun setUp() {
        repository = mock(OperationsRepository::class.java)
        service = ContainersService(repository)
    }

    @Test
    fun `getAllByClientId returns containers when present`() {
        val container = Operation.Container(containerId)
        val op = mock(Operation::class.java)
        `when`(op.containers).thenReturn(listOf(container))
        `when`(repository.findAllByClientId(clientId)).thenReturn(listOf(op))

        val result = service.getAllByClientId(clientId)

        assertEquals(listOf(container), result)
    }

    @Test
    fun `getAllByClientId returns empty list when no emails`() {
        `when`(repository.findAllByClientId(clientId)).thenReturn(emptyList())

        val result = service.getAllByClientId(clientId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllByClientId skips null containers`() {
        val op = mock(Operation::class.java)
        `when`(op.containers).thenReturn(null)
        `when`(repository.findAllByClientId(clientId)).thenReturn(listOf(op))

        val result = service.getAllByClientId(clientId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getOrdersByContainerId returns orders from repository`() {
        val order = Operation.Order("o1", null)
        `when`(repository.getOrdersByContainerId(clientId, containerId)).thenReturn(listOf(order))

        val result = service.getOrdersByContainerId(clientId, containerId)

        assertEquals(listOf(order), result)
    }

    @Test
    fun `getOrdersByContainerId returns empty list when no orders`() {
        `when`(repository.getOrdersByContainerId(clientId, containerId)).thenReturn(emptyList())

        val result = service.getOrdersByContainerId(clientId, containerId)

        assertTrue(result.isEmpty())
    }

}