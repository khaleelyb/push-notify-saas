package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.SupabaseApi
import com.example.data.Website
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed class DeveloperState {
    object Idle : DeveloperState()
    object Loading : DeveloperState()
    data class Success(val websites: List<Website>) : DeveloperState()
    data class Error(val message: String) : DeveloperState()
}

class DeveloperViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow<DeveloperState>(DeveloperState.Idle)
    val state: StateFlow<DeveloperState> = _state

    private val supabaseApi: SupabaseApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
        retrofit.create(SupabaseApi::class.java)
    }

    @SuppressLint("HardwareIds")
    private val ownerId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    fun loadWebsites() {
        viewModelScope.launch {
            _state.value = DeveloperState.Loading
            try {
                val response = supabaseApi.getWebsites(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    bearerToken = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                    ownerId = "eq.$ownerId"
                )
                if (response.isSuccessful) {
                    _state.value = DeveloperState.Success(response.body() ?: emptyList())
                } else {
                    _state.value = DeveloperState.Error("Failed to load: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = DeveloperState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createWebsite(id: String, name: String) {
        viewModelScope.launch {
            try {
                val response = supabaseApi.createWebsite(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    bearerToken = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                    website = Website(id, name, ownerId)
                )
                if (response.isSuccessful) {
                    loadWebsites()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteWebsite(id: String) {
        viewModelScope.launch {
            try {
                val response = supabaseApi.deleteWebsite(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    bearerToken = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                    id = "eq.$id"
                )
                if (response.isSuccessful) {
                    loadWebsites()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
