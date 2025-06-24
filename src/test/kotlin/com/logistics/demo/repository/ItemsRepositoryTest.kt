package com.logistics.demo.repository

import com.logistics.demo.domain.Item
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemsRepositoryTest {

    private lateinit var dynamoDbClient: DynamoDbClient
    private lateinit var itemsRepository: ItemsRepository

    @BeforeAll
    fun setUp() {
        dynamoDbClient = mock(DynamoDbClient::class.java)
        itemsRepository = ItemsRepository(dynamoDbClient)
    }

    @Test
    fun `getById returns item when found`() {
        val id = "ITEM-1"
        val type = "TYPE-A"
        val itemMap = mapOf(
            "id" to AttributeValue.fromS(id),
            "type" to AttributeValue.fromS(type)
        )
        val getItemResponse = GetItemResponse.builder().item(itemMap).build()
        `when`(dynamoDbClient.getItem(any(GetItemRequest::class.java))).thenReturn(getItemResponse)

        val result = itemsRepository.getById(id)
        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals(type, result.type)
    }

    @Test
    fun `getById returns null when not found`() {
        val getItemResponse = GetItemResponse.builder().item(emptyMap()).build()
        `when`(dynamoDbClient.getItem(any(GetItemRequest::class.java))).thenReturn(getItemResponse)

        val result = itemsRepository.getById("NOT-FOUND")
        assertNull(result)
    }

    @Test
    fun `save inserts new item when not exists`() {
        val item = Item("ITEM-2", "TYPE-B")
        // getById returns null (not found)
        val getItemResponse = GetItemResponse.builder().item(emptyMap()).build()
        `when`(dynamoDbClient.getItem(any(GetItemRequest::class.java))).thenReturn(getItemResponse)
        // putItem returns successfully
        `when`(dynamoDbClient.putItem(any(PutItemRequest::class.java))).thenReturn(PutItemResponse.builder().build())

        val result = itemsRepository.save(item)
        assertEquals(item, result)
        verify(dynamoDbClient).putItem(any(PutItemRequest::class.java))
    }

    @Test
    fun `save throws when item already exists`() {
        val item = Item("ITEM-3", "TYPE-C")
        val itemMap = mapOf(
            "id" to AttributeValue.fromS(item.id),
            "type" to AttributeValue.fromS(item.type)
        )
        val getItemResponse = GetItemResponse.builder().item(itemMap).build()
        `when`(dynamoDbClient.getItem(any(GetItemRequest::class.java))).thenReturn(getItemResponse)

        assertThrows<IllegalArgumentException> {
            itemsRepository.save(item)
        }
        verify(dynamoDbClient, never()).putItem(any(PutItemRequest::class.java))
    }
}