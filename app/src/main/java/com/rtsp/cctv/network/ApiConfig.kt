package com.rtsp.cctv.network

object ApiConfig {
    const val BASE_URL = "https://lisandro.sytes.net/cam/" 
}

fun snapshotUrl(cameraId: Int): String {
    return "${ApiConfig.BASE_URL}cameras/$cameraId/snapshot"
}
