package com.logistics.demo.repository

import com.logistics.demo.domain.Operation
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import java.util.UUID

@Service
class Operations(private val dynamoDbClient: DynamoDbClient) {

    fun save(request: Operation) {
        val item = mapOf(
            "id" to AttributeValue.fromS(UUID.randomUUID().toString()),
            "client_id" to AttributeValue.fromS(request.clientId),
            "booking" to AttributeValue.fromS(request.booking),
            "containers" to AttributeValue.fromS(
                request.containers?.joinToString(", ") { it.container } ?: ""
            ),
            "orders" to AttributeValue.fromS(
                request.orders?.joinToString(", ") { it.purchase } ?: ""
            ),
        )
        val putRequest = PutItemRequest.builder()
            .tableName("EmailRequests")
            .item(item)
            .build()
        dynamoDbClient.putItem(putRequest)
    }

    fun findAllByClientId(clientId: String): List<Operation> {
        val scanRequest = software.amazon.awssdk.services.dynamodb.model.ScanRequest.builder()
            .tableName("EmailRequests")
            .filterExpression("client_id = :clientId")
            .expressionAttributeValues(mapOf(":clientId" to AttributeValue.fromS(clientId)))
            .build()

        val result = dynamoDbClient.scan(scanRequest)

        return result.items().map { item ->
            Operation(
                clientId = item["client_id"]?.s() ?: "",
                booking = item["booking"]?.s() ?: "",
                containers = item["containers"]?.s()?.split(", ")?.map { Operation.Container(it) },
                orders = item["orders"]?.s()?.split(", ")?.map { Operation.Order(it) }
            )
        }
    }

    fun findByClientIdAndBookingId(clientId: String, bookingId: String): Operation? {

        val key = mapOf(
            "client_id" to AttributeValue.fromS(clientId),
            "booking" to AttributeValue.fromS(bookingId)
        )
        val getRequest = software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName("EmailRequests")
            .key(key)
            .build()

        val result = dynamoDbClient.getItem(getRequest)
        val item = result.item()
        return if (item != null && item.isNotEmpty()) {

            Operation(
                clientId = item["client_id"]?.s() ?: "",
                booking = item["booking"]?.s() ?: "",
                containers = item["containers"]?.s()?.split(", ")?.map { Operation.Container(it) },
                orders = item["orders"]?.s()?.split(", ")?.map { Operation.Order(it) }
            )
        } else {
            null
        }
    }

    fun getContainersByPurchaseId(clientID: String, purchaseId: String): List<Operation.Container> {
        return findAllByClientId(clientID)
            .filter { operation ->
                operation.orders?.any { it.purchase == purchaseId } == true
            }
            .flatMap { it.containers ?: emptyList() }
    }

    fun getOrdersByContainerId(clientID: String, containerId: String): List<Operation.Order> {
        return findAllByClientId(clientID)
            .filter { operation ->
                operation.containers?.any { it.container == containerId } == true
            }
            .flatMap { it.orders ?: emptyList() }
    }
}