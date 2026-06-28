package com.example.data

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    suspend fun insert(subscription: SubscriptionEntity) {
        subscriptionDao.insert(subscription)
    }

    suspend fun delete(websiteId: String) {
        subscriptionDao.deleteByWebsiteId(websiteId)
    }

    suspend fun exists(websiteId: String): Boolean {
        return subscriptionDao.exists(websiteId)
    }
}
