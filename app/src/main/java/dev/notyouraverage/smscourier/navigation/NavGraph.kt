package dev.notyouraverage.smscourier.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.notyouraverage.smscourier.composables.components.SessionBreakdownBottomSheet
import dev.notyouraverage.smscourier.composables.screens.AddDeviceScreen
import dev.notyouraverage.smscourier.composables.screens.ArchiveManagementScreen
import dev.notyouraverage.smscourier.composables.screens.DeviceHistoryScreen
import dev.notyouraverage.smscourier.composables.screens.ForwardingControlScreen
import dev.notyouraverage.smscourier.composables.screens.HomeScreen
import dev.notyouraverage.smscourier.composables.screens.PairedDevicesScreen
import dev.notyouraverage.smscourier.composables.screens.PairingRequestsScreen
import dev.notyouraverage.smscourier.composables.screens.SessionHistoryScreen
import dev.notyouraverage.smscourier.composables.screens.SettingsScreen
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.repository.ForwardedMessageRepository
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import dev.notyouraverage.smscourier.services.SmsSender
import dev.notyouraverage.smscourier.viewmodels.AddDeviceViewModel
import dev.notyouraverage.smscourier.viewmodels.ArchiveManagementViewModel
import dev.notyouraverage.smscourier.viewmodels.DeviceHistoryViewModel
import dev.notyouraverage.smscourier.viewmodels.ForwardingControlViewModel
import dev.notyouraverage.smscourier.viewmodels.HomeViewModel
import dev.notyouraverage.smscourier.viewmodels.PairedDevicesViewModel
import dev.notyouraverage.smscourier.viewmodels.PairingRequestsViewModel
import dev.notyouraverage.smscourier.viewmodels.SessionHistoryViewModel
import dev.notyouraverage.smscourier.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder

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
    val messageRepository = remember {
        ForwardedMessageRepository(
            database,
            database.forwardedMessageDao(),
            database.forwardingSessionDao(),
            database.pairedDeviceDao(),
        )
    }
    val exportManager = remember {
        ExportManager(sessionRepository, messageRepository)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Home.route) {
            var showSessionBreakdown by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(deviceRepository, sessionRepository),
            )

            // Session breakdown bottom sheet
            if (showSessionBreakdown) {
                val homeState by homeViewModel.homeState.collectAsState()
                SessionBreakdownBottomSheet(
                    sessions = homeState.directionalStatus.activeSessions,
                    onDismiss = { showSessionBreakdown = false },
                    onStopSession = { session ->
                        scope.launch {
                            sessionRepository.endSession(session.id, "USER")
                        }
                    },
                )
            }

            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPairedDevices = {
                    navController.navigate(Screen.PairedDevices.route)
                },
                onNavigateToPairingRequests = {
                    navController.navigate(Screen.PairingRequests.route)
                },
                onNavigateToAddDevice = {
                    navController.navigate(Screen.AddDevice.route)
                },
                onNavigateToDeviceHistory = {
                    navController.navigate(Screen.DeviceHistory.route)
                },
                onNavigateToForwardingControl = {
                    navController.navigate(Screen.ForwardingControl.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onShowSessionBreakdown = { showSessionBreakdown = true },
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
                factory = SettingsViewModel.Factory(
                    application = context.applicationContext as Application,
                    settingsRepository = settingsRepository,
                    forwardingSessionRepository = sessionRepository,
                ),
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.DeviceHistory.route) {
            val viewModel: DeviceHistoryViewModel = viewModel(
                factory = DeviceHistoryViewModel.Factory(
                    deviceRepository,
                    sessionRepository,
                    exportManager,
                ),
            )
            DeviceHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSessionHistory = { phoneNumber, role ->
                    navController.navigate(Screen.SessionHistory.createRoute(phoneNumber, role))
                },
                onNavigateToArchiveManagement = { phoneNumber, role ->
                    navController.navigate(Screen.ArchiveManagement.createRoute(phoneNumber, role))
                },
            )
        }

        composable(
            route = Screen.ArchiveManagement.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            // URL decode phone number (was encoded in Screen.ArchiveManagement.createRoute)
            val phoneNumber = URLDecoder.decode(
                backStackEntry.arguments?.getString("phoneNumber") ?: return@composable,
                "UTF-8",
            )
            val role = backStackEntry.arguments?.getString("role") ?: return@composable

            val viewModel: ArchiveManagementViewModel = viewModel(
                factory = ArchiveManagementViewModel.Factory(
                    phoneNumber = phoneNumber,
                    role = role,
                    deviceRepository = deviceRepository,
                    exportManager = exportManager,
                ),
            )
            ArchiveManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.SessionHistory.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            // URL decode phone number (was encoded in Screen.SessionHistory.createRoute)
            val phoneNumber = URLDecoder.decode(
                backStackEntry.arguments?.getString("phoneNumber") ?: return@composable,
                "UTF-8",
            )
            val role = backStackEntry.arguments?.getString("role") ?: return@composable

            val viewModel: SessionHistoryViewModel = viewModel(
                factory = SessionHistoryViewModel.Factory(
                    sessionRepository = sessionRepository,
                    messageRepository = messageRepository,
                    exportManager = exportManager,
                    phoneNumber = phoneNumber,
                    deviceRole = role,
                ),
            )
            SessionHistoryScreen(
                viewModel = viewModel,
                messageRepository = messageRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
