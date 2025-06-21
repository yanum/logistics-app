package com.logistics.demo.domain

data class User(
    val userName: String,
    val password: String,
    val clientId: String,
    val isActive: Boolean = true,
    val isAdmin: Boolean = false,
    val lastUpdateAt: String,
)