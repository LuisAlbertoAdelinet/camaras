package com.rtsp.cctv.network

import com.rtsp.cctv.BuildConfig
import com.rtsp.cctv.data.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiClient(tokenStore: TokenStore) {
    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore.getToken()
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}
