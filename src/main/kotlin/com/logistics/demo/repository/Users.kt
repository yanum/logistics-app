package com.logistics.demo.repository

import com.logistics.demo.controller.dto.UserRequest
import com.logistics.demo.domain.User
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import java.time.Instant
import java.util.*

@Service
class Users(private val dynamoDbClient: DynamoDbClient) {

    fun getByUserName(username: String): User? {
        val key = mapOf("user_name" to AttributeValue.fromS(username))
        val getRequest = software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName("Users")
            .key(key)
            .build()
        val result = dynamoDbClient.getItem(getRequest)
        val item = result.item()
        return if (item != null && item.isNotEmpty()) {
            User(
                userName = item["user_name"]?.s() ?: "",
                password = item["password"]?.s() ?: "",
                clientId = item["client_id"]?.s() ?: "",
                isActive = item["is_active"]?.bool() ?: true,
                isAdmin = item["is_admin"]?.bool() ?: false,
                lastUpdateAt = item["last_update_at"]?.s() ?: ""
            )
        } else {
            null
        }
    }


    fun save(request: UserRequest): User {
        // Check if user already exists
        val existingUser = getByUserName(request.username)
        if (existingUser != null) {
            throw IllegalArgumentException("User with username '${request.username}' already exists.")
        }

        val user = User(
            userName = request.username,
            password = request.password,
            clientId = "client-${UUID.randomUUID()}",
            isActive = request.isActive,
            isAdmin = request.isAdmin,
            lastUpdateAt = Instant.now().toString()
        )

        val item = mapOf(
            "user_name" to AttributeValue.fromS(user.userName),
            "password" to AttributeValue.fromS(user.password),
            "client_id" to AttributeValue.fromS(user.clientId),
            "is_active" to AttributeValue.fromBool(user.isActive),
            "is_admin" to AttributeValue.fromBool(user.isAdmin),
            "last_update_at" to AttributeValue.fromS(user.lastUpdateAt)
        )

        val putRequest = PutItemRequest.builder()
            .tableName("Users")
            .item(item)
            .build()

        dynamoDbClient.putItem(putRequest)

        return user
    }
}
