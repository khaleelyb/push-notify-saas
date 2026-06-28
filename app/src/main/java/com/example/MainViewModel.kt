package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.SupabaseApi
import com.example.data.Subscriber
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed class SubscriptionState {
    object Idle : SubscriptionState()
    object Loading : SubscriptionState()
    data class Success(val websiteId: String) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}

class MainViewModel : ViewModel() {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Idle)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val supabaseApi: SupabaseApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
        retrofit.create(SupabaseApi::class.java)
    }

    fun subscribe(websiteId: String) {
        viewModelScope.launch {
            _subscriptionState.value = SubscriptionState.Loading
            try {
                val token = FirebaseMessaging.getInstance().getToken().await()
                val response = supabaseApi.registerSubscriber(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    bearerToken = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                    subscriber = Subscriber(websiteId, token)
                )

                if (response.isSuccessful) {
                    _subscriptionState.value = SubscriptionState.Success(websiteId)
                } else {
                    _subscriptionState.value = SubscriptionState.Error("Failed to subscribe: ${response.code()}")
                }
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
