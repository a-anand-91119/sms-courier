# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-01-15

### Added

- Bidirectional device pairing (source can forward TO paired devices, target can receive FROM)
- Composite primary key architecture for paired devices
- Pending pairing state with countdown UI
- Password creation dialog for pairing approval
- Role-specific UNPAIR command handling
- Session management with start/stop forwarding
- Foreground service for reliable SMS forwarding

### Changed

- Segmented button shows device counts per role
- Service toggle with loading state feedback

### Fixed

- Password dialog button text truncation
- Tab label clarity improvements
