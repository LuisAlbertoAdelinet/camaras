package com.rtsp.cctv.network

import com.rtsp.cctv.data.CameraListResponse
import com.rtsp.cctv.data.LoginResponse
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(
    val username: String,
    val password: String
)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("cameras")
    suspend fun getCameras(): CameraListResponse

    @GET("cameras/{id}")
    suspend fun getCamera(@Path("id") id: Int): com.rtsp.cctv.data.Camera
}
