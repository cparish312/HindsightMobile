package com.connor.hindsightmobile.obj

import com.connor.hindsightmobile.utils.Preferences
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object RetrofitClient {
    private var apiKey: String = Preferences.prefs.getString(Preferences.apikey, "")
        .toString()
    fun getInstance(baseUrl: String, connectTimeout: Long = 10, numTries: Int = 1): Retrofit
    {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS) // Set the connection timeout
            .readTimeout(30, TimeUnit.SECONDS) // Set the read timeout
            .writeTimeout(60, TimeUnit.SECONDS) // Set the write timeout
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest = originalRequest.newBuilder()
                    .header("Hightsight-API-Key", apiKey)
                    .build()
                chain.proceed(newRequest)
            }
            .addInterceptor(RetryInterceptor(numTries))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .client(client)
            .build()
    }

    class RetryInterceptor(private val numTries: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            var response: Response? = null
            var attempt = 0

            while (attempt < numTries) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful) {
                        return response
                    }
                } catch (e: IOException) {
                    // Retry on network errors
                }

                attempt++
                if (attempt >= numTries) {
                    throw IOException("Max retries exceeded")
                }
            }
            return response ?: chain.proceed(request)
        }
    }
}


