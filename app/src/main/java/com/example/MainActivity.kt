package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AddWebsiteDialog
import com.example.ui.DashboardScreen
import com.example.ui.DeveloperDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val developerViewModel: DeveloperViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DeveloperViewModel(applicationContext) as T
            }
        }
    }

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Permission denied — FCM token fetch will succeed but notifications
            // won't be shown on screen. We surface this in the UI via the ViewModel.
            mainViewModel.setPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                                label = { Text("Explore") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { 
                                    selectedTab = 1
                                    developerViewModel.loadWebsites()
                                },
                                icon = { Icon(Icons.Default.DeveloperMode, contentDescription = null) },
                                label = { Text("Manage") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> UserDashboard(
                            viewModel = mainViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> {
                            val devState by developerViewModel.state.collectAsState()
                            DeveloperDashboardScreen(
                                state = devState,
                                onCreateClick = { id, name -> developerViewModel.createWebsite(id, name) },
                                onDeleteClick = { id -> developerViewModel.deleteWebsite(id) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "pushnotify" && data.host == "subscribe") {
            val websiteId = data.getQueryParameter("website_id")
            if (websiteId != null) {
                mainViewModel.subscribe(websiteId)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun UserDashboard(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.subscriptionState.collectAsState()
    val subscriptions by viewModel.allSubscriptions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        DashboardScreen(
            subscriptions = subscriptions,
            onAddClick = { showAddDialog = true },
            onDeleteClick = { viewModel.unsubscribe(it) }
        )

        if (showAddDialog) {
            AddWebsiteDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = {
                    viewModel.subscribe(it)
                    showAddDialog = false
                }
            )
        }

        if (state !is SubscriptionState.Idle) {
            SubscriptionOverlay(
                state = state,
                onDismiss = { viewModel.resetState() }
            )
        }
    }
}

@Composable
fun SubscriptionOverlay(
    state: SubscriptionState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (state !is SubscriptionState.Loading) {
                Button(onClick = onDismiss) {
                    Text("Got it")
                }
            }
        },
        text = {
            SubscriptionScreen(state = state)
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun SubscriptionScreen(state: SubscriptionState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Subscription Status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = state is SubscriptionState.Loading) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(visible = state is SubscriptionState.Success) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Successfully subscribed!",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(visible = state is SubscriptionState.Error) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (state as SubscriptionState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}