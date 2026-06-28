package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Subscriber(
    @param:Json(name = "website_id") val websiteId: String,
    @param:Json(name = "fcm_token") val fcmToken: String
)
