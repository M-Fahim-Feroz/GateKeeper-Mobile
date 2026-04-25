# GateKeeper Mobile Requirements

This document outlines the detailed system, development, and operational requirements for the GateKeeper Mobile application.

## 1. Development Requirements

To build and contribute to the GateKeeper Mobile app, your development environment must meet the following specifications:

### Core Tooling
* **IDE**: Android Studio Ladybug (2024.2+) or newer.
* **JDK**: Java Development Kit (JDK) 17 is strictly required.
* **Kotlin**: Version `2.1.0`.
* **Gradle**: version `8.13.2` (managed by the wrapper).

### Code & SDK Requirements
* **Minimum SDK**: API Level 26 (Android 8.0 Oreo).
* **Target SDK**: API Level 35 (Android 15).
* **Compile SDK**: API Level 35.

---

## 2. Hardware & Device Requirements

### End-User Device
* **OS**: Android 8.0 or higher.
* **Permissions**:
  * `VpnService` permission (user-granted) for firewall and DNS filtering.
  * Network Access (`INTERNET`) for backend communication.
  * Notification permissions for real-time alerts.
* **Hardware**: Any standard Android smartphone.
* **Storage**: Minimum 50MB free space.

### Testing Devices
* **VPN Capabilities**: It is highly recommended to use **physical Android devices** for testing the core VPN and networking features, as the Android Emulator has known limitations and bugs when handling the `VpnService` API and complex packet routing.

---

## 3. Architecture & Libraries (Software Requirements)

The application architecture requires the following primary technical stack (dependencies automatically resolved via Gradle):

### UI & Presentation
* **Jetpack Compose BOM**: `2024.12.01`
* **Material 3**: `1.3.1`
* **Vico Charts**: `2.0.0-beta.2` (for traffic monitoring analytics)
* **Coil**: `2.7.0` (for asynchronous image loading)

### Core Architecture (MVVM + Clean Architecture)
* **Hilt (Dagger)**: `2.53.1` for Dependency Injection.
* **AndroidX Lifecycle & ViewModels**: `2.8.7`
* **AndroidX Navigation**: `2.8.5`
* **Kotlin Coroutines**: `1.9.0` for asynchronous background tasks.

### Local Storage & Databases
* **Room Database**: `2.6.1` (SQLite abstraction for rules, logs, etc.)
* **Jetpack DataStore**: `1.1.1` (Preferences)

### Networking & Security
* **Retrofit**: `2.11.0` (for API communications)
* **OkHttp3**: `4.12.0`
* **MaxMind GeoIP2**: `4.2.0` (for IP location lookups)
* **Android `VpnService` API**: Custom packet filtering, TUN interface management, and DNS sinkhole implementation.

---

## 4. Integration & Backend Requirements

While the local Firewall and DNS filtering functions operate autonomously, some advanced features require a backend connection:
* **AI Assistant & Remote Control**: Requires connection to the **GateKeeper-Agent** (FastAPI backend). The mobile app must be on the same network or configured to hit the correct remote host.
* **Threat Intelligence Feeds**: The app requires internet connectivity to periodically sync global blocklists (if centralized) or relies on the backend to push new rules.
