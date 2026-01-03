package com.rtsp.cctv.network

import com.rtsp.cctv.data.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton ApiClient - Single instance shared across all screens
 * Optimized with connection pooling and timeouts
 */
object ApiClientSingleton {
    @Volatile
    private var instance: ApiService? = null
    @Volatile
    private var currentToken: String? = null
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    fun getInstance(tokenStore: TokenStore): ApiService {
        val token = tokenStore.getToken()
        
        // Recreate if token changed (login/logout)
        if (token != currentToken) {
            synchronized(this) {
                if (token != currentToken) {
                    currentToken = token
                    instance = createApiService(tokenStore)
                }
            }
        }
        
        return instance ?: synchronized(this) {
            instance ?: createApiService(tokenStore).also { instance = it }
        }
    }
    
    private fun createApiService(tokenStore: TokenStore): ApiService {
        val authInterceptor = Interceptor { chain ->
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

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            // Optimized timeouts
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // Connection pooling - reuse connections
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
    
    /**
     * Clear instance on logout
     */
    fun clear() {
        synchronized(this) {
            instance = null
            currentToken = null
        }
    }
}

// Keep old class for backward compatibility during migration
class ApiClient(tokenStore: TokenStore) {
    val api: ApiService = ApiClientSingleton.getInstance(tokenStore)
}
