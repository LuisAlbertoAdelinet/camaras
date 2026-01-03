package com.rtsp.cctv.network

import com.rtsp.cctv.data.Camera
import com.rtsp.cctv.data.CameraListResponse
import com.rtsp.cctv.data.LoginResponse
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.DELETE

data class LoginRequest(
    val username: String,
    val password: String
)

data class ChangePasswordRequest(
    val new_password: String
)

data class Snapshot(
    val id: Int,
    val user_id: Int,
    val camera_id: Int,
    val camera_name: String,
    val filename: String,
    val created_at: String
)

data class Recording(
    val id: Int,
    val user_id: Int,
    val camera_id: Int,
    val camera_name: String,
    val filename: String,
    val duration: Int,
    val created_at: String
)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("me")
    suspend fun getProfile(): Map<String, Any>

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Map<String, Any>

    @GET("cameras")
    suspend fun getCameras(): CameraListResponse

    @GET("cameras/{id}")
    suspend fun getCamera(@Path("id") id: Int): Camera

    @POST("cameras/{id}/snapshot/save")
    suspend fun saveSnapshot(@Path("id") id: Int): Map<String, Any>

    @retrofit2.http.Multipart
    @POST("cameras/{id}/snapshot/upload")
    suspend fun uploadSnapshot(
        @Path("id") id: Int,
        @retrofit2.http.Part image: okhttp3.MultipartBody.Part
    ): Map<String, Any>

    @GET("snapshots")
    suspend fun getSnapshots(): Map<String, List<Snapshot>>

    @GET("snapshots/{id}/image")
    fun getSnapshotImageUrl(@Path("id") id: Int): String

    @DELETE("snapshots/{id}")
    suspend fun deleteSnapshot(@Path("id") id: Int): Map<String, Any>

    // Recording Endpoints
    @POST("cameras/{id}/recording/start")
    suspend fun startRecording(@Path("id") id: Int): Map<String, Any>

    @POST("cameras/{id}/recording/stop")
    suspend fun stopRecording(@Path("id") id: Int): Map<String, Any>

    @GET("recordings")
    suspend fun getRecordings(): Map<String, List<Recording>>

    @DELETE("recordings/{id}")
    suspend fun deleteRecording(@Path("id") id: Int): Map<String, Any>
}
