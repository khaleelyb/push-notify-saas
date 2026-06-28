package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE websiteId = :websiteId")
    suspend fun deleteByWebsiteId(websiteId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE websiteId = :websiteId)")
    suspend fun exists(websiteId: String): Boolean
}
