package com.rtsp.cctv.network

import com.rtsp.cctv.BuildConfig

fun snapshotUrl(cameraId: Int): String {
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return "$base/cameras/$cameraId/snapshot"
}
