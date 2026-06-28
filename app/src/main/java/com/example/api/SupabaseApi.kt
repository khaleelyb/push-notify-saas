package com.example.api

import com.example.data.Subscriber
import com.example.data.Website
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {
    @POST("rest/v1/subscribers")
    suspend fun registerSubscriber(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=minimal",
        @Body subscriber: Subscriber
    ): Response<Unit>

    @GET("rest/v1/websites")
    suspend fun getWebsiteById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") id: String
    ): Response<List<Website>>

    @GET("rest/v1/websites")
    suspend fun getWebsites(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("owner_id") ownerId: String
    ): Response<List<Website>>

    @POST("rest/v1/websites")
    suspend fun createWebsite(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=minimal",
        @Body website: Website
    ): Response<Unit>

    @PATCH("rest/v1/websites")
    suspend fun updateWebsite(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Query("id") id: String,
        @Body websiteUpdate: Map<String, String>
    ): Response<Unit>

    @DELETE("rest/v1/websites")
    suspend fun deleteWebsite(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") id: String
    ): Response<Unit>
}