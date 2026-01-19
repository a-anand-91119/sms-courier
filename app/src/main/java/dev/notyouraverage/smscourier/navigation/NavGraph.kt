package dev.notyouraverage.smscourier.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.notyouraverage.smscourier.composables.screens.SettingsScreen
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import dev.notyouraverage.smscourier.services.SmsSender
import dev.notyouraverage.smscourier.viewmodels.AddDeviceViewModel
import dev.notyouraverage.smscourier.viewmodels.ForwardingControlViewModel
import dev.notyouraverage.smscourier.viewmodels.HomeViewModel
import dev.notyouraverage.smscourier.viewmodels.PairedDevicesViewModel
import dev.notyouraverage.smscourier.viewmodels.PairingRequestsViewModel
import dev.notyouraverage.smscourier.viewmodels.SettingsViewModel

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
    val settingsRepository = remember { SettingsRepository(context) }

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
                factory = PairedDevicesViewModel.Factory(
                    deviceRepository,
                    sessionRepository,
                    smsSender,
                    settingsRepository,
                ),
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
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(settingsRepository),
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
