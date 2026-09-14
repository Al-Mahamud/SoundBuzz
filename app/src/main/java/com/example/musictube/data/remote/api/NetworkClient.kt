package com.example.musictube.data.remote.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val diagnosticsInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val cleanUrl = request.url.toString().replace(Regex("key=[^&]+"), "key=***")
            try {
                val response = chain.proceed(request)
                val peekedBody = try {
                    response.peekBody(16 * 1024).string()
                } catch (e: Exception) {
                    "Unable to read body: ${e.message}"
                }
                com.example.musictube.utils.DiagnosticsLogger.logApi(
                    method = request.method,
                    url = cleanUrl,
                    statusCode = response.code,
                    summary = "HTTP ${response.code} ${response.message}",
                    rawResponse = peekedBody,
                    isSuccess = response.isSuccessful
                )
                response
            } catch (e: Exception) {
                com.example.musictube.utils.DiagnosticsLogger.logApi(
                    method = request.method,
                    url = cleanUrl,
                    statusCode = -1,
                    summary = "Network Error: ${e.message}",
                    rawResponse = e.stackTraceToString(),
                    isSuccess = false
                )
                throw e
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(diagnosticsInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val apiService: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)
    }
}
