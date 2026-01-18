package dev.notyouraverage.smscourier.integration

/**
 * Marker annotation for integration tests.
 *
 * Use to filter tests in CI:
 * - Run integration tests only: `./gradlew test --tests "*Integration*"`
 * - Exclude integration tests: via Gradle configuration
 *
 * Usage:
 * ```
 * @IntegrationTest
 * class PairingFlowIntegrationTest : IntegrationTestBase() {
 *     // ...
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegrationTest
