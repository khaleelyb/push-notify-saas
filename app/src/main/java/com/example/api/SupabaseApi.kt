package com.example.api

import com.example.data.Subscriber
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseApi {
    @POST("rest/v1/subscribers")
    suspend fun registerSubscriber(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=minimal",
        @Body subscriber: Subscriber
    ): Response<Unit>
}
