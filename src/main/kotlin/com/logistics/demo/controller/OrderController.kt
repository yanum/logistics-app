package com.logistics.demo.controller

import TokenUtils.extractClientIdFromJwt
import com.logistics.demo.controller.dto.ApiResponse
import com.logistics.demo.domain.Operation
import com.logistics.demo.domain.Operation.Order
import com.logistics.demo.domain.service.OrdersService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val ordersService: OrdersService) {

    @GetMapping
    fun getOrders(@RequestHeader("Authorization") authorizationHeader: String)
    : ResponseEntity<ApiResponse<List<Order>>> {
        return try {
            val clientID = extractClientIdFromJwt(authorizationHeader)
                ?: throw IllegalArgumentException("Invalid or missing Authorization header")
            ResponseEntity.ok(ApiResponse(data = ordersService.getAllByClientId(clientID)))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(error = e.message))
        }
    }


    @GetMapping("/{purchaseId}/containers")
    fun getContainersByPurchaseId(
        @RequestHeader("Authorization") authorizationHeader: String,
        @PathVariable purchaseId: String
    ): ResponseEntity<ApiResponse<List<Operation.Container>>> {
        return try {
            val clientID = extractClientIdFromJwt(authorizationHeader)
                ?: throw IllegalArgumentException("Invalid or missing Authorization header")
            val containers = ordersService.getContainersByPurchaseId(clientID, purchaseId)
            ResponseEntity.ok(ApiResponse(data = containers))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(error = e.message))
        }
    }

}
