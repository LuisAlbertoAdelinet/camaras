package com.rtsp.cctv.network

object ApiConfig {
    const val BASE_URL = "https://lisandro.sytes.net/cam/" 
}

fun snapshotUrl(cameraId: Int, token: String? = null): String {
    val url = "${ApiConfig.BASE_URL}cameras/$cameraId/snapshot"
    return if (token != null) "$url?token=$token" else url
}
