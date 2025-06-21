package com.logistics.demo.domain

data class Item (
    val id: String,
    val type: String,
) {

    enum class ItemType(val value: String) {
        CONTAINER("container"),
        INVOICE("invoice"),
        PURCHASE("purchase");
    }
}