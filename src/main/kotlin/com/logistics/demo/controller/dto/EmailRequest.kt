package com.logistics.demo.controller.dto

data class EmailRequest(
    val booking: String,
    val containers: List<ContainerDto>? = null,
    val orders: List<OrderDto>? = null
) {

    data class ContainerDto(
        val container: String
    )

    data class OrderDto(
        val purchase: String,
        val invoices: List<InvoiceDto>? = null
    )

    data class InvoiceDto(
        val invoice: String
    )
}