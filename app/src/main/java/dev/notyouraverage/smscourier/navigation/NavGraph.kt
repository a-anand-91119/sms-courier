package dev.notyouraverage.smscourier.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.notyouraverage.smscourier.composables.screens.AddDeviceScreen
import dev.notyouraverage.smscourier.composables.screens.ForwardingControlScreen
import dev.notyouraverage.smscourier.composables.screens.HomeScreen
import dev.notyouraverage.smscourier.composables.screens.PairedDevicesScreen
import dev.notyouraverage.smscourier.composables.screens.PairingRequestsScreen
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.services.SmsSender
import dev.notyouraverage.smscourier.viewmodels.AddDeviceViewModel
import dev.notyouraverage.smscourier.viewmodels.ForwardingControlViewModel
import dev.notyouraverage.smscourier.viewmodels.HomeViewModel
import dev.notyouraverage.smscourier.viewmodels.PairedDevicesViewModel
import dev.notyouraverage.smscourier.viewmodels.PairingRequestsViewModel

@Composable
fun SmsCourierNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
) {
    val context = LocalContext.current

    // Create shared dependencies
    val database = remember { SmsCourierDatabase.getDatabase(context) }
    val deviceRepository = remember { PairedDeviceRepository(database.pairedDeviceDao()) }
    val sessionRepository = remember { ForwardingSessionRepository(database.forwardingSessionDao()) }
    val smsSender = remember { SmsSender(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(deviceRepository, sessionRepository),
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPairedDevices = {
                    navController.navigate(Screen.PairedDevices.route)
                },
                onNavigateToPairingRequests = {
                    navController.navigate(Screen.PairingRequests.route)
                },
                onNavigateToAddDevice = {
                    navController.navigate(Screen.AddDevice.route)
                },
                onNavigateToForwardingControl = {
                    navController.navigate(Screen.ForwardingControl.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(Screen.PairedDevices.route) {
            val viewModel: PairedDevicesViewModel = viewModel(
                factory = PairedDevicesViewModel.Factory(deviceRepository, sessionRepository, smsSender),
            )
            PairedDevicesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddDevice = {
                    navController.navigate(Screen.AddDevice.route)
                },
                onDeviceClick = { device ->
                    // Could navigate to device detail screen
                },
            )
        }

        composable(Screen.PairingRequests.route) {
            val viewModel: PairingRequestsViewModel = viewModel(
                factory = PairingRequestsViewModel.Factory(deviceRepository, smsSender),
            )
            PairingRequestsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.AddDevice.route) {
            val viewModel: AddDeviceViewModel = viewModel(
                factory = AddDeviceViewModel.Factory(deviceRepository, smsSender),
            )
            AddDeviceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ForwardingControl.route) {
            val viewModel: ForwardingControlViewModel = viewModel(
                factory = ForwardingControlViewModel.Factory(
                    context,
                    deviceRepository,
                    sessionRepository,
                    smsSender,
                ),
            )
            ForwardingControlScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            // Simple settings placeholder
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Settings coming soon",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
