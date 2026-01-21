# Contributing to SMS Courier

Thank you for your interest in contributing to SMS Courier!

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 17 or higher
- Android SDK with API 34+

### Setup

```bash
git clone https://gitlab.notyouraverage.dev/a.anand.91119/sms-courier.git
cd sms-courier
```

Open the project in Android Studio and let Gradle sync.

### Build & Test

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

## Code Style

We use [ktlint](https://github.com/pinterest/ktlint) via Spotless for consistent formatting.

```bash
# Check formatting
./gradlew spotlessCheck

# Auto-fix formatting
./gradlew spotlessApply
```

**Always run `./gradlew spotlessApply` before committing.**

## Pull Request Process

1. **Fork** the repository
2. **Create a branch** from `main` (`git checkout -b feature/your-feature`)
3. **Make your changes** with clear, focused commits
4. **Run tests** and formatting (`./gradlew test spotlessApply`)
5. **Push** your branch and open a Pull Request

### Commit Messages

Use clear, descriptive commit messages:

```
feat: add message filtering by sender
fix: correct session expiration calculation
docs: update README installation steps
refactor: simplify pairing state machine
test: add integration tests for forwarding
```

Prefix types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

## Reporting Issues

When reporting bugs, please include:

- Android version and device model
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs (if available)

## Feature Requests

Open an issue describing:

- The problem you're trying to solve
- Your proposed solution
- Alternative approaches you've considered

## Architecture Overview

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed documentation, including:

- SMS command protocol
- Service architecture
- Data layer design
- Security implementation

## Questions?

Open an issue with your question and we'll do our best to help.
