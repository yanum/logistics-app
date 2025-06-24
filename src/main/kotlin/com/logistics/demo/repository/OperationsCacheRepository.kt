package com.logistics.demo.repository

import com.logistics.demo.domain.Operation
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class OperationsCacheRepository {
    private val cache = ConcurrentHashMap<String, Operation>()

    fun cache(operation: Operation) {
        cache["${operation.clientId}:${operation.booking}"] = operation
    }

    fun cacheAll(operations: List<Operation>) {
        operations.forEach { cache(it) }
    }

    fun findAllByClientId(clientId: String): List<Operation> {
        return cache.values.filter { it.clientId == clientId }
    }

    fun findByClientIdAndBookingId(clientId: String, bookingId: String): Operation? {
        return cache["$clientId:$bookingId"]
    }
}