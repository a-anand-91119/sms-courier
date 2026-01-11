package dev.notyouraverage.otpcourier.enums

enum class SmsCommand {
    // Legacy commands
    START,
    STOP,
    SEND_TO_WORKER,

    // Pairing commands
    PAIR_REQUEST,
    PAIR_APPROVED,
    PAIR_REJECTED,
    UNPAIR,

    // Forwarding commands
    START_FORWARD,
    STOP_FORWARD,
    FORWARD_DATA,
    FORWARD_SMS,
}
