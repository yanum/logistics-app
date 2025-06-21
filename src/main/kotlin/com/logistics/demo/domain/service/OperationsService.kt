package com.logistics.demo.domain.service

import com.logistics.demo.controller.dto.EmailRequest
import com.logistics.demo.domain.Item
import com.logistics.demo.domain.Item.ItemType
import com.logistics.demo.domain.Operation
import com.logistics.demo.repository.Items
import com.logistics.demo.repository.Operations
import org.springframework.stereotype.Component

@Component
class OperationsService(private val operationsRepository: Operations, private val itemsRepository: Items) {

    fun save(request: EmailRequest, clientId: String): Operation {
        val existingOperation = operationsRepository.findByClientIdAndBookingId(clientId, request.booking)
        if (existingOperation != null) {
            return updateBooking(existingOperation, request)
        }

        val uniqueItems = validateRequestItems(request)

        val operation = Operation(
            clientId = clientId,
            booking = request.booking,
            containers = request.containers?.map { Operation.Container(it.container) },
            orders = request.orders?.map { order ->
                Operation.Order(
                    purchase = order.purchase,
                    invoices = order.invoices?.map { Operation.Invoice(it.invoice) }
                )
            }
        )
        operationsRepository.save(operation)
        uniqueItems.forEach { itemsRepository.save(it) }

        return operation
    }

    private fun updateBooking(existingOperation: Operation, request: EmailRequest): Operation {
        val updatedContainers = (existingOperation.containers.orEmpty() +
                (request.containers?.map { Operation.Container(it.container) } ?: emptyList()))
            .distinctBy { it.container }

        val updatedOrders = (existingOperation.orders.orEmpty() +
                (request.orders?.map { order ->
                    Operation.Order(
                        purchase = order.purchase,
                        invoices = order.invoices?.map { Operation.Invoice(it.invoice) }
                    )
                } ?: emptyList()))
            .distinctBy { it.purchase }

        val updatedOperation = existingOperation.copy(
            containers = updatedContainers,
            orders = updatedOrders
        )

        operationsRepository.save(updatedOperation)
        return updatedOperation
    }

    private fun validateRequestItems(request: EmailRequest): Set<Item> {
        val requestedItems: MutableList<Item> = mutableListOf()
        request.containers?.map { Item(id = it.container, type = ItemType.CONTAINER.name) }
            ?.let { requestedItems.addAll(it) }
        request.orders?.map { Item(id = it.purchase, type = ItemType.PURCHASE.name) }?.let { requestedItems.addAll(it) }
        request.orders?.flatMap {
            it.invoices?.map { invoice ->
                Item(
                    id = invoice.invoice,
                    type = ItemType.INVOICE.name
                )
            } ?: emptyList()
        }
            ?.let { requestedItems.addAll(it) }

        val requestedItemsSet = requestedItems.toSet()

        if (requestedItemsSet.size != requestedItems.size) {
            throw IllegalArgumentException("Duplicate items found in the request")
        }

        requestedItemsSet.forEach {
            itemsRepository.getById(it.id)?.let { item ->
                if (item.type == it.type) {
                    throw IllegalArgumentException("Some items already exist with the same type")
                }
            }
        }

        return requestedItemsSet
    }
}