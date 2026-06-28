package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Website(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "owner_id") val ownerId: String
)
