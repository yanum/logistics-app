package com.logistics.demo.domain.service

import com.logistics.demo.domain.Operation
import com.logistics.demo.repository.Operations
import org.springframework.stereotype.Component

@Component
class ContainersService(private val repository: Operations) {

    fun getAllByClientId(clientId: String): List<Operation.Container> {
        val emails = repository.findAllByClientId(clientId)
        return emails.flatMap { it.containers ?: emptyList() }
    }

    fun getOrdersByContainerId(clientId: String, containerId: String): List<Operation.Order> {
        return repository.getOrdersByContainerId(clientId, containerId)
    }
}