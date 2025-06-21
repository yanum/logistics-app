package com.logistics.demo.domain

data class Operation(
    val clientId: String,
    val booking: String,
    val containers: List<Container>? = null,
    val orders: List<Order>? = null
) {

    data class Container(
        val container: String
    )

    data class Order(
        val purchase: String,
        val invoices: List<Invoice>? = null
    )

    data class Invoice(
        val invoice: String
    )
}