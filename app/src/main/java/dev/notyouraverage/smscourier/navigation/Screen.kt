package dev.notyouraverage.smscourier.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object PairedDevices : Screen("paired_devices")
    data object PairingRequests : Screen("pairing_requests")
    data object AddDevice : Screen("add_device")
    data object ForwardingControl : Screen("forwarding_control")
    data object Settings : Screen("settings")

    // Screen with arguments
    data object DeviceDetail : Screen("device_detail/{phoneNumber}") {
        fun createRoute(phoneNumber: String): String = "device_detail/$phoneNumber"
    }

    data object ApproveDevice : Screen("approve_device/{phoneNumber}") {
        fun createRoute(phoneNumber: String): String = "approve_device/$phoneNumber"
    }

    data object StartForwarding : Screen("start_forwarding/{phoneNumber}") {
        fun createRoute(phoneNumber: String): String = "start_forwarding/$phoneNumber"
    }
}
