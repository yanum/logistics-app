package com.logistics.demo.controller

import TokenUtils
import com.logistics.demo.controller.UserController
import com.logistics.demo.controller.dto.AuthenticationRequest
import com.logistics.demo.controller.dto.UserRequest
import com.logistics.demo.domain.User
import com.logistics.demo.repository.UsersRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserControllerTest {
    private lateinit var usersRepository: UsersRepository
    private lateinit var controller: UserController
    private val fixedInstant = "2024-01-01T00:00:00Z"
    private val fixedClientId = "client-123"

    @BeforeEach
    fun setUp() {
        usersRepository = mockk()
        controller = UserController(usersRepository)
        mockkObject(TokenUtils)
        mockkStatic(Instant::class)
        every { Instant.now().toString() } returns fixedInstant
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(usersRepository)
        unmockkAll()
    }

    @Test
    fun `save returns success with valid user request`() {
        val request = UserRequest(
            userName = "testuser",
            password = "password123"
        )
        val savedUser = User(
            userName = request.userName,
            password = request.password,
            clientId = fixedClientId,
            lastUpdateAt = fixedInstant
        )

        every { usersRepository.save(request) } returns savedUser

        val response = controller.save(request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(savedUser, response.body!!.data)

        verify(exactly = 1) { usersRepository.save(request) }
    }

    @Test
    fun `save handles validation error`() {
        val request = UserRequest(
            userName = "",
            password = ""
        )
        every { usersRepository.save(request) } throws IllegalArgumentException("Invalid user data")

        val response = controller.save(request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body?.error)
        assertEquals("Invalid user data", response.body!!.error)

        verify(exactly = 1) { usersRepository.save(request) }
    }

    @Test
    fun `login returns success with valid credentials`() {
        val request = AuthenticationRequest(
            username = "testuser",
            password = "password123"
        )
        val user = User(
            userName = "testuser",
            password = "password123",
            clientId = fixedClientId,
            lastUpdateAt = fixedInstant
        )
        val token = "generated-jwt-token"

        every { usersRepository.getByUserName(request.username) } returns user
        every { TokenUtils.generateToken(user.clientId) } returns token

        val response = controller.login(request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(token, response.body!!.data)

        verify(exactly = 1) {
            usersRepository.getByUserName(request.username)
            TokenUtils.generateToken(user.clientId)
        }
    }

    @Test
    fun `login returns unauthorized with invalid password`() {
        val request = AuthenticationRequest(
            username = "testuser",
            password = "wrongpassword"
        )
        val user = User(
            userName = "testuser",
            password = "password123",
            clientId = fixedClientId,
            lastUpdateAt = fixedInstant
        )

        every { usersRepository.getByUserName(request.username) } returns user

        val response = controller.login(request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body?.error)
        assertEquals("Invalid credentials", response.body!!.error)

        verify(exactly = 1) { usersRepository.getByUserName(request.username) }
        verify(exactly = 0) { TokenUtils.generateToken(any()) }
    }

    @Test
    fun `login returns unauthorized with non-existent user`() {
        val request = AuthenticationRequest(
            username = "nonexistent",
            password = "password123"
        )
        every { usersRepository.getByUserName(request.username) } returns null

        val response = controller.login(request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body?.error)
        assertEquals("Invalid credentials", response.body!!.error)

        verify(exactly = 1) { usersRepository.getByUserName(request.username) }
        verify(exactly = 0) { TokenUtils.generateToken(any()) }
    }

    @Test
    fun `login handles unexpected error`() {
        val request = AuthenticationRequest(
            username = "testuser",
            password = "password123"
        )
        every { usersRepository.getByUserName(request.username) } throws RuntimeException("Database error")

        val response = controller.login(request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body?.error)
        assertEquals("Database error", response.body!!.error)

        verify(exactly = 1) { usersRepository.getByUserName(request.username) }
        verify(exactly = 0) { TokenUtils.generateToken(any()) }
    }
}