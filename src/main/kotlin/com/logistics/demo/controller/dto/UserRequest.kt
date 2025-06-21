package com.logistics.demo.controller.dto

import java.time.Instant

data class UserRequest(
    val username: String,
    val password: String,
    val isActive: Boolean = true,
    val isAdmin: Boolean = false,
    val lastUpdateAt: String = Instant.now().toString(),
)