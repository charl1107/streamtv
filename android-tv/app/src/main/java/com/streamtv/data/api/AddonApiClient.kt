package com.streamtv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AddonApiClient {

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Cache Retrofit instances per base URL to avoid re-creating them
    private val apiCache = mutableMapOf<String, AddonApi>()

    @Synchronized
    fun createApi(baseUrl: String): AddonApi {
        val baseUrlFormatted = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return apiCache.getOrPut(baseUrlFormatted) {
            Retrofit.Builder()
                .baseUrl(baseUrlFormatted)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AddonApi::class.java)
        }
    }
}
