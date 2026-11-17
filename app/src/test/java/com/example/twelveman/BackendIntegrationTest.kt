package com.example.twelveman

import com.example.twelveman.data.api.ApiService
import com.example.twelveman.data.api.models.LoginRequest
import com.example.twelveman.data.api.models.RegisterRequest
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class BackendIntegrationTest {

    private lateinit var apiService: ApiService

    @Before
    fun setup() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().create()

        // JVM tests run on host localhost
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost/12THMAN_BACKEND/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Ensure testuser2026 exists for idempotent tests
        runBlocking {
            try {
                apiService.register(
                    RegisterRequest(
                        fullName = "Test User",
                        email = "testuser2026@example.com",
                        phone = "9876543210",
                        password = "Test@123456",
                        role = "PLAYER"
                    )
                )
            } catch (ignored: Exception) { }
        }
    }

    @Test
    fun testDatabaseConnectionEndpoint() = runBlocking {
        val response = apiService.testDb()
        assertTrue("HTTP response should be successful", response.isSuccessful)
        val body = response.body()
        assertNotNull("Body should not be null", body)
        assertEquals("success", body?.get("status"))
        assertEquals("12thman_db", body?.get("database"))
    }

    @Test
    fun testLoginWithSeedUser() = runBlocking {
        val request = LoginRequest(
            email = "user@example.com",
            password = "password123"
        )
        val response = apiService.login(request)
        assertTrue("Login should succeed", response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("success", body?.status)
        assertEquals("user@example.com", body?.data?.email)
        assertEquals("Rahul Dravid", body?.data?.fullName)
        assertEquals("ORGANIZER", body?.data?.role)
        assertNotNull(body?.data?.token)
    }

    @Test
    fun testLoginWithTestUser() = runBlocking {
        val request = LoginRequest(
            email = "testuser2026@example.com",
            password = "Test@123456"
        )
        val response = apiService.login(request)
        assertTrue("Login should succeed", response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("success", body?.status)
        assertEquals("testuser2026@example.com", body?.data?.email)
        assertNotNull(body?.data?.token)
    }

    @Test
    fun testLoginWithInvalidPassword() = runBlocking {
        val request = LoginRequest(
            email = "testuser2026@example.com",
            password = "IncorrectPassword999"
        )
        val response = apiService.login(request)
        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun testDuplicateRegistrationRejection() = runBlocking {
        val request = RegisterRequest(
            fullName = "Duplicate Test",
            email = "testuser2026@example.com",
            phone = "9876543210",
            password = "Test@123456",
            role = "PLAYER"
        )
        val response = apiService.register(request)
        assertEquals(409, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun testFetchPlayersFromMySQL() = runBlocking {
        val response = apiService.getPlayers()
        assertTrue("Player API call should be successful", response.isSuccessful)
        val players = response.body()?.data
        assertNotNull("Players list must not be null", players)
        assertTrue("Should return players from MySQL", players!!.isNotEmpty())
        assertEquals("Virat Sharma", players[0].name)
    }

    @Test
    fun testFetchUmpiresFromMySQL() = runBlocking {
        val response = apiService.getUmpires()
        assertTrue("Umpires API call should be successful", response.isSuccessful)
        val umpires = response.body()?.data
        assertNotNull("Umpires list must not be null", umpires)
        assertTrue("Should return umpires from MySQL", umpires!!.isNotEmpty())
        assertEquals("Suresh Menon", umpires[0].name)
    }

    @Test
    fun testFetchTournamentsFromMySQL() = runBlocking {
        val response = apiService.getTournaments()
        assertTrue("Tournaments API call should be successful", response.isSuccessful)
        val tournaments = response.body()?.data
        assertNotNull("Tournaments list must not be null", tournaments)
        assertTrue("Should return tournaments from MySQL", tournaments!!.isNotEmpty())
    }

    @Test
    fun testFetchBookingsFromMySQL() = runBlocking {
        val response = apiService.getBookings()
        assertTrue("Bookings API call should be successful", response.isSuccessful)
        val bookings = response.body()?.data
        assertNotNull("Bookings list must not be null", bookings)
        assertTrue("Should return bookings from MySQL", bookings!!.isNotEmpty())
    }
}
