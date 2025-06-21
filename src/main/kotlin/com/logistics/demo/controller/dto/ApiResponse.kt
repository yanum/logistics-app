package com.logistics.demo.controller.dto

data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null
)