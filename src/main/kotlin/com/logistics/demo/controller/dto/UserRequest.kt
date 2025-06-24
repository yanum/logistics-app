package com.logistics.demo.controller.dto

import java.time.Instant

data class UserRequest(
    val userName: String,
    val password: String,
    val lastUpdateAt: String = Instant.now().toString(),
)