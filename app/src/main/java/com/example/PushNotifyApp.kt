package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SubscriptionRepository

class PushNotifyApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { SubscriptionRepository(database.subscriptionDao()) }
}
