package com.logistics.demo.repository

import com.logistics.demo.domain.Item
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest

@Service
class ItemsRepository(
    private val dynamoDbClient: DynamoDbClient,
) {

    fun getById(id: String): Item? {
        val key = mapOf("id" to AttributeValue.fromS(id))
        val getRequest = software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName("Items")
            .key(key)
            .build()
        val result = dynamoDbClient.getItem(getRequest)
        val item = result.item()
        return if (item != null && item.isNotEmpty()) {
            Item(
                id = item["id"]?.s() ?: "",
                type = item["type"]?.s() ?: "",
            )
        } else {
            null
        }
    }


    fun save(request: Item): Item {
        val existingItem = getById(request.id)
        if (existingItem != null) {
            throw IllegalArgumentException("Item '${request}' already exists.")
        }

        val item = mapOf(
            "id" to AttributeValue.fromS(request.id),
            "type" to AttributeValue.fromS(request.type),
        )

        val putRequest = PutItemRequest.builder()
            .tableName("Items")
            .item(item)
            .build()

        dynamoDbClient.putItem(putRequest)

        return request
    }
}
