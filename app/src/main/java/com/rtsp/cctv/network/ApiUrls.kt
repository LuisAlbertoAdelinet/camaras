package com.rtsp.cctv.network

fun snapshotUrl(cameraId: Int): String {
    val base = ApiConfig.BASE_URL.trimEnd('/')
    return "$base/cameras/$cameraId/snapshot"
}
