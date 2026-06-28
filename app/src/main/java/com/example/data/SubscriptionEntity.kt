package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val websiteId: String,
    val siteName: String,
    val subscribedAt: Long = System.currentTimeMillis()
)
