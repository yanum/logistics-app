
  package com.logistics.demo.repository

  import com.logistics.demo.domain.Operation
  import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
  import io.github.resilience4j.retry.annotation.Retry
  import io.github.resilience4j.bulkhead.annotation.Bulkhead
  import org.slf4j.LoggerFactory
  import org.springframework.stereotype.Service
  import software.amazon.awssdk.services.dynamodb.DynamoDbClient
  import software.amazon.awssdk.services.dynamodb.model.*
  import java.util.UUID

  @Service
  class OperationsRepository(
      private val dynamoDbClient: DynamoDbClient,
      private val cacheRepository: OperationsCacheRepository,
  ) {
      private val logger = LoggerFactory.getLogger(this::class.java)

      companion object {
          private const val TABLE_NAME = "EmailRequests"
          private const val CLIENT_ID_INDEX = "client_id-index"
          private const val CIRCUIT_BREAKER_NAME = "operationsRepository"
      }

      @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
      @Retry(name = CIRCUIT_BREAKER_NAME)
      @Bulkhead(name = CIRCUIT_BREAKER_NAME)
      fun save(request: Operation) {
          try {
              val item = createItemMap(request)
              val putRequest = PutItemRequest.builder()
                  .tableName(TABLE_NAME)
                  .item(item)
                  .build()

              dynamoDbClient.putItem(putRequest)
          } catch (e: Exception) {
              throw RepositoryException("Failed to save operation", e)
          }
      }

      @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "findAllByClientIdFallback")
      @Retry(name = CIRCUIT_BREAKER_NAME)
      @Bulkhead(name = CIRCUIT_BREAKER_NAME)
      fun findAllByClientId(clientId: String): List<Operation> {
          try {
              val queryRequest = QueryRequest.builder()
                  .tableName(TABLE_NAME)
                  .indexName(CLIENT_ID_INDEX)
                  .keyConditionExpression("client_id = :clientId")
                  .expressionAttributeValues(mapOf(":clientId" to AttributeValue.fromS(clientId)))
                  .build()

              return dynamoDbClient.query(queryRequest)
                  .items()
                  .map { mapToOperation(it) }
                  .also { cacheRepository.cacheAll(it) }
          } catch (e: Exception) {
              logger.error("Error finding operations by clientId: ${e.message}", e)
              return findAllByClientIdFallback(clientId, e)
          }
      }

      @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "findByClientIdAndBookingIdFallback")
      @Retry(name = CIRCUIT_BREAKER_NAME)
      fun findByClientIdAndBookingId(clientId: String, bookingId: String): Operation? {
          try {
              val key = mapOf(
                  "client_id" to AttributeValue.fromS(clientId),
                  "booking" to AttributeValue.fromS(bookingId)
              )
              val getRequest = GetItemRequest.builder()
                  .tableName(TABLE_NAME)
                  .key(key)
                  .build()

              return dynamoDbClient.getItem(getRequest)
                  .item()
                  ?.takeIf { it.isNotEmpty() }
                  ?.let { mapToOperation(it) }
                  ?.also { cacheRepository.cache(it) }
          } catch (e: Exception) {
              logger.error("Error finding operation: ${e.message}", e)
              return findByClientIdAndBookingIdFallback(clientId, bookingId, e)
          }
      }

      private fun createItemMap(request: Operation) = mapOf(
          "id" to AttributeValue.fromS(UUID.randomUUID().toString()),
          "client_id" to AttributeValue.fromS(request.clientId),
          "booking" to AttributeValue.fromS(request.booking),
          "containers" to AttributeValue.fromS(
              request.containers?.joinToString(", ") { it.container } ?: ""
          ),
          "orders" to AttributeValue.fromS(
              request.orders?.joinToString(", ") { it.purchase } ?: ""
          )
      )

      private fun mapToOperation(item: Map<String, AttributeValue>) = Operation(
          clientId = item["client_id"]?.s() ?: "",
          booking = item["booking"]?.s() ?: "",
          containers = item["containers"]?.s()?.split(", ")?.filter { it.isNotEmpty() }?.map { Operation.Container(it) },
          orders = item["orders"]?.s()?.split(", ")?.filter { it.isNotEmpty() }?.map { Operation.Order(it) }
      )

      // Fallback methods

      private fun findAllByClientIdFallback(clientId: String, e: Exception): List<Operation> {
          logger.warn("Falling back to cache for findAllByClientId", e)
          return cacheRepository.findAllByClientId(clientId)
      }

      private fun findByClientIdAndBookingIdFallback(clientId: String, bookingId: String, e: Exception): Operation? {
          logger.warn("Falling back to cache for findByClientIdAndBookingId", e)
          return cacheRepository.findByClientIdAndBookingId(clientId, bookingId)
      }



    fun getContainersByPurchaseId(clientID: String, purchaseId: String): List<Operation.Container> {
        val queryRequest = QueryRequest.builder()
            .tableName("EmailRequests")
            .keyConditionExpression("client_id = :clientId")
            .filterExpression("contains(orders, :purchaseId)")
            .expressionAttributeValues(
                mapOf(
                    ":clientId" to AttributeValue.fromS(clientID),
                    ":purchaseId" to AttributeValue.fromS(purchaseId)
                )
            )
            .build()

        val result = dynamoDbClient.query(queryRequest)

        return result.items().flatMap { item ->
            item["containers"]?.s()?.split(", ")?.map { Operation.Container(it) } ?: emptyList()
        }
    }

    fun getOrdersByContainerId(clientID: String, containerId: String): List<Operation.Order> {
        return findAllByClientId(clientID)
            .filter { operation ->
                operation.containers?.any { it.container == containerId } == true
            }
            .flatMap { it.orders ?: emptyList() }
    }
}

  class RepositoryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
