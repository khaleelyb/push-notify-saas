package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.SupabaseApi
import com.example.data.Subscriber
import com.example.data.SubscriptionEntity
import com.example.data.SubscriptionRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

sealed class SubscriptionState {
    object Idle : SubscriptionState()
    object Loading : SubscriptionState()
    data class Success(val websiteId: String) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}

class MainViewModel(private val repository: SubscriptionRepository) : ViewModel() {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Idle)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    val allSubscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
                // Check if already subscribed locally
                if (repository.exists(websiteId)) {
                    _subscriptionState.value = SubscriptionState.Success(websiteId)
                    return@launch
                }

                // Fetch website name from API so we display it properly in the list
                val websiteName = try {
                    val websiteResponse = supabaseApi.getWebsiteById(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        bearerToken = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                        id = "eq.$websiteId"
                    )
                    websiteResponse.body()?.firstOrNull()?.name ?: websiteId
                } catch (e: Exception) {
                    websiteId // fallback to ID if lookup fails
                }

                val token = try {
                    FirebaseMessaging.getInstance().getToken().await()
                } catch (e: Exception) {
                    _subscriptionState.value = SubscriptionState.Error(
                        "FCM registration failed: ${e.javaClass.simpleName}: ${e.message ?: "could not get device token"}. " +
                        "Please verify your google-services.json matches your applicationId and Firebase is fully configured."
                    )
                    return@launch
                }

                val response = supabaseApi.registerSubscriber(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    bearerToken = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                    subscriber = Subscriber(websiteId, token)
                )

                if (response.isSuccessful) {
                    repository.insert(SubscriptionEntity(websiteId, websiteName))
                    _subscriptionState.value = SubscriptionState.Success(websiteId)
                } else {
                    _subscriptionState.value = SubscriptionState.Error("Failed to subscribe: ${response.code()}")

                }
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun unsubscribe(websiteId: String) {
        viewModelScope.launch {
            try {
                // In a real app, you would also notify Supabase to remove the token
                repository.delete(websiteId)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun resetState() {
        _subscriptionState.value = SubscriptionState.Idle
    }

    fun setPermissionDenied() {
        // Only surface if not already in a meaningful state (e.g. mid-subscribe)
        if (_subscriptionState.value is SubscriptionState.Idle) {
            _subscriptionState.value = SubscriptionState.Error(
                "Notification permission denied. You won't receive push notifications."
            )
        }
    }
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PushNotifyApp
                return MainViewModel(application.repository) as T
            }
        }
    }
}