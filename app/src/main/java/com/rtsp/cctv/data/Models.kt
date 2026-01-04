package com.rtsp.cctv.data

import com.squareup.moshi.Json

data class Camera(
    val id: Int,
    val name: String,
    @Json(name = "rtsp_url") val rtspUrl: String,
    val channel: Int,
    val location: String,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "group_name") val group_name: String? = "General"
)

data class User(
    val id: Int,
    val username: String,
    val role: String
)

data class LoginResponse(
    val token: String,
    val role: String
)

data class CameraListResponse(
    val items: List<Camera>
)
