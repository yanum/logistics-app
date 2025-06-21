package com.logistics.demo.domain.service

import com.logistics.demo.domain.Operation
import com.logistics.demo.repository.Operations
import org.springframework.stereotype.Component

@Component
class OrdersService(private val repository: Operations) {

    fun getAllByClientId(clientId: String): List<Operation.Order> {
        val emails = repository.findAllByClientId(clientId)
        return emails.flatMap { it.orders ?: emptyList() }
    }

    fun getContainersByPurchaseId(clientId: String, purchaseId: String): List<Operation.Container> {
        return repository.getContainersByPurchaseId(clientId, purchaseId)
    }
}