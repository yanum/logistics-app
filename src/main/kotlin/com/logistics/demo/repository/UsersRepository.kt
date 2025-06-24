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
class UsersRepository(private val dynamoDbClient: DynamoDbClient) {

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
                lastUpdateAt = item["last_update_at"]?.s() ?: ""
            )
        } else {
            null
        }
    }


    fun save(request: UserRequest): User {
        val existingUser = getByUserName(request.userName)
        if (existingUser != null) {
            throw IllegalArgumentException("User with username '${request.userName}' already exists.")
        }

        val user = User(
            userName = request.userName,
            password = request.password,
            clientId = "client-${UUID.randomUUID()}",
            lastUpdateAt = Instant.now().toString()
        )

        val item = mapOf(
            "user_name" to AttributeValue.fromS(user.userName),
            "password" to AttributeValue.fromS(user.password),
            "client_id" to AttributeValue.fromS(user.clientId),
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
