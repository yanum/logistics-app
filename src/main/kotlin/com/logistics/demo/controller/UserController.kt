package com.logistics.demo.controller

import TokenUtils.generateToken
import com.logistics.demo.controller.dto.ApiResponse
import com.logistics.demo.controller.dto.AuthenticationRequest
import com.logistics.demo.controller.dto.UserRequest
import com.logistics.demo.domain.User
import com.logistics.demo.repository.Users
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@RestController
@RequestMapping("/api/users")
class UserController(private val users: Users) {

    @PostMapping("/create")
    fun save(@RequestBody request: UserRequest): ResponseEntity<ApiResponse<User>> {
        return try {
            ResponseEntity.ok(ApiResponse(data= users.save(request)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(error = e.message))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthenticationRequest): ResponseEntity<ApiResponse<String>> {
        return try {
            val user = users.getByUserName(request.username)
            if (user != null && user.password == request.password) {
                val token = generateToken(user.clientId)
                ResponseEntity.ok(ApiResponse(data = token))
            } else {
                ResponseEntity.status(401).body(ApiResponse(error = "Invalid credentials"))
            }
        } catch (e: Exception) {
            ResponseEntity.status(500).body(ApiResponse(error = e.message))
        }
    }

}



