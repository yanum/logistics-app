package com.logistics.demo.controller

import TokenUtils.extractClientIdFromJwt
import com.logistics.demo.controller.dto.ApiResponse
import com.logistics.demo.controller.dto.EmailRequest
import com.logistics.demo.domain.Operation
import com.logistics.demo.domain.service.OperationsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@RestController
@RequestMapping("/api/emails")
class EmailController(private val operationsService: OperationsService) {

    @PostMapping
    fun sendEmail(
        @RequestBody request: EmailRequest,
        @RequestHeader("Authorization") authorizationHeader: String,
    ): ResponseEntity<ApiResponse<Operation>> {
        return try {
            val clientID = extractClientIdFromJwt(authorizationHeader)
                ?: throw IllegalArgumentException("Invalid or missing Authorization header")
            ResponseEntity.ok(ApiResponse(data = operationsService.save(request, clientID)))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(error = e.message))
        }
    }
}

