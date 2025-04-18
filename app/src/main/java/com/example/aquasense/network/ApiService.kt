package com.example.aquasense.network

import com.example.aquasense.AquariumRequest
import com.example.aquasense.models.Aquarium
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class LoginRequest(val username: String, val password: String)
data class AuthRequest(val username: String, val password: String)


data class LoginResponse(val token: String)
data class AuthResponse(val token: String)

data class AquariumDataResponse(
    val temperature: Float,
    val waterLevel: Int,
    val lightOn: Boolean
)

data class Measurement(
    val id: Long,
    val aquariumId: Long,
    val timestamp: String,
    val temperature: Double,
    val lightStatus: Boolean,
    val lastFeedTime: String
)

interface ApiService {

    @GET("aquarium")
    suspend fun getAquariums(): Response<List<Aquarium>>

    @POST("aquarium/create")
    suspend fun createAquarium(@Body aquariumRequest: AquariumRequest): Response<Aquarium>

    @DELETE("aquarium/delete/{id}")
    suspend fun deleteAquarium(@Path("id") id: Long): Response<String>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("aquarium/{aquariumId}/measurements")
    suspend fun getAquariumMeasurements(@Path("aquariumId") aquariumId: Long): Response<List<Measurement>>

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("IoT/feed/{aquariumId}")
    suspend fun feedFish(@Path("aquariumId") aquariumId: Long): Response<String>

    @POST("IoT/turnLight/{aquariumId}")
    suspend fun turnLight(@Path("aquariumId") aquariumId: Long): Response<String>
}