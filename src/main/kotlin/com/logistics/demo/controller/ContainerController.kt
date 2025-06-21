package com.logistics.demo.controller

import TokenUtils.extractClientIdFromJwt
import com.logistics.demo.controller.dto.ApiResponse
import com.logistics.demo.domain.Operation
import com.logistics.demo.domain.service.ContainersService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.GetMapping

@RestController
@RequestMapping("/api/containers")
class ContainerController(private val containersService: ContainersService) {

    @GetMapping
    fun getContainers(@RequestHeader("Authorization") authorizationHeader: String)
    : ResponseEntity<ApiResponse<List<Operation.Container>>> {
        return try {
            val clientID = extractClientIdFromJwt(authorizationHeader)
                ?: throw IllegalArgumentException("Invalid or missing Authorization header")
            ResponseEntity.ok(ApiResponse(data = containersService.getAllByClientId(clientID)))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(error = e.message))
        }
    }

    @GetMapping("/{containerId}/orders")
    fun getOrdersByContainerId(
        @RequestHeader("Authorization") authorizationHeader: String,
        @org.springframework.web.bind.annotation.PathVariable containerId: String
    ): ResponseEntity<ApiResponse<List<Operation.Order>>> {
        return try {
            val clientID = extractClientIdFromJwt(authorizationHeader)
                ?: throw IllegalArgumentException("Invalid or missing Authorization header")
            val orders = containersService.getOrdersByContainerId(clientID, containerId)
            ResponseEntity.ok(ApiResponse(data = orders))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(error = e.message))
        }
    }
}


