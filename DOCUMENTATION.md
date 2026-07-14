# GateKeeper Mobile — Comprehensive Project Documentation

<div align="center">
  <h3>🛡️ Android Network Security Suite</h3>
  <p>Built as part of an Advanced Network Security Final Year Project</p>
</div>

---

## 1. Project Overview

**GateKeeper Mobile** is a rootless Android security application that intercepts and inspects all device network traffic using Android's `VpnService` API. It provides system-wide protection without requiring root access, combining:

| Capability | Description |
|---|---|
| Per-App Firewall | Block specific apps from using Wi-Fi or Mobile Data, with schedule support |
| DNS Sinkhole | Block ad, tracking, and malware domains at the DNS layer |
| Traffic Monitor | Real-time per-app bandwidth (BandwidthMonitor) and connection logging |
| Threat Intelligence | IP & domain blacklists from public threat feeds |
| Evil Twin Detection | Rogue Wi-Fi access point detection via BSSID tracking |
| IMSI Catcher Detection | Alerts on suspicious cellular downgrade to 2G |
| Data Exfiltration Detection | Correlates large uploads with recent sensor (mic/camera) use |
| Certificate Auditor | Scans user-installed CA certificates for known MITM proxies |
| Permission Auditor | Risk-scores installed apps based on declared permissions |
| PCAP Capture | Captures and rotates full packet captures for analysis |

**Platform**: Android 8.0+ (API 26+)  
**Language**: Kotlin 2.2.10  
**UI**: Jetpack Compose with Material 3  
**Architecture**: Clean Architecture + MVVM  
**Implementation Maturity**: Core features, database operations, and UI are fully implemented. Bidirectional packet forwarding via TCP/UDP relays is active. There are currently no automated tests in the repository.

---

## 2. Current Project Structure

```
com.gatekeeper.mobile/
├── GateKeeperApp.kt              # Hilt entry point, notification channels
├── di/                           # Dependency Injection (AppModule.kt)
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt        # Room database (v12) and Migrations
│   │   ├── dao/                  # 9 DAOs
│   │   └── entity/               # 9 Entities
│   ├── model/
│   │   └── BuiltInBlocklists.kt  # Hardcoded feeds
│   ├── remote/                   # Retrofit API
│   └── repository/               # Repositories including DataStore settings
├── domain/
│   ├── model/                    # Domain models
│   └── usecase/                  # Use cases (e.g. ScanWifiNetworksUseCase)
├── notifications/
│   └── GKNotificationManager.kt  # Helper for VPN & Alert notifications
├── receiver/
│   ├── BootReceiver.kt           # Auto-starts VPN
│   └── GateKeeperDeviceAdminReceiver.kt # Device Admin capabilities
├── ui/
│   ├── MainActivity.kt           
│   ├── navigation/               # AppNavigation.kt, Screen.kt
│   ├── screens/                  
│   │   ├── alerts/               # Unresolved and historical security alerts
│   │   ├── certaudit/            
│   │   ├── dashboard/            # Main landing screen
│   │   ├── dns/                  
│   │   ├── firewall/             
│   │   ├── onboarding/           
│   │   ├── permissionauditor/    
│   │   ├── protecthub/           # Central hub for protection modules
│   │   ├── settings/             
│   │   ├── threats/              
│   │   ├── traffic/              
│   │   └── wifiscanner/          
│   ├── components/               
│   └── theme/                    
├── util/                         
│   ├── GeoIpLookup.kt            
│   └── NetworkUtils.kt           
└── vpn/                          # Core VPN Engine
    ├── GateKeeperVpnService.kt   # TUN interface orchestrator
    ├── PacketFilter.kt           
    ├── DnsResolver.kt            
    ├── DnsBlocklistManager.kt    
    ├── ConnectionTracker.kt      
    ├── TrafficLogger.kt          
    ├── BandwidthMonitor.kt       # Live traffic speed aggregator
    ├── ThreatFeedManager.kt      
    ├── RogueApDetector.kt        
    ├── CellularMonitor.kt        
    ├── ExfiltrationDetector.kt   
    ├── CertificateAuditor.kt     
    ├── PrivacyAccessLogger.kt    
    ├── PermissionScanner.kt      
    ├── WifiScanner.kt            
    ├── GeoIpResolver.kt          
    ├── NetworkUtils.kt           
    ├── NetworkAttributionMapper.kt 
    ├── PcapWriter.kt             
    ├── ExportUtils.kt            
    ├── TlsSniExtractor.kt        
    └── relay/                    # Bidirectional Forwarding
        ├── TcpRelayHandler.kt    
        ├── TcpSession.kt         
        └── UdpRelayHandler.kt    
```

---

## 3. Architecture

### 3.1 High-Level Design

The app uses a single-activity architecture (`MainActivity`) with Jetpack Compose.
- **UI Layer**: Compose screens paired with `@HiltViewModel`s.
- **Domain Layer**: Clean Architecture UseCases for encapsulating complex logic (e.g., getting apps, scanning Wi-Fi).
- **Data Layer**: Room Database for relational data, DataStore for key-value preferences, Retrofit for network calls.
- **Core Engine (VPN Service)**: Runs in the same process. It exposes state via `StateFlow`s for direct consumption by the UI, bypassing traditional repositories for high-frequency updates (e.g., live bandwidth).

### 3.2 Packet Forwarding Architecture
Unlike simpler DNS-only VPNs, GateKeeper implements **Bidirectional Forwarding** for apps explicitly blocked or inspected. 
1. `PacketFilter` determines verdicts (`ALLOW`, `DROP`, `DNS_SINKHOLE`, `DNS_INTERCEPT`, `DNS_LEAK`).
2. `GateKeeperVpnService` routes `ALLOW` IPv4 packets to `TcpRelayHandler` (TCP) or `UdpRelayHandler` (UDP) in `vpn/relay/`.
3. These relays map sockets to standard Android sockets via `VpnService.protect()`, enabling full traffic interception and forwarding without root.

---

## 4. Build Configuration

Based on actual `app/build.gradle.kts` and `gradle/libs.versions.toml`:

* **Namespace**: `com.gatekeeper.mobile`
* **Application ID**: `com.gatekeeper.mobile` (Debug: `com.gatekeeper.mobile.debug`)
* **Version Code**: 1
* **Version Name**: 1.0.0
* **Minimum SDK**: 26 (Android 8.0)
* **Target SDK**: 35 (Android 15)
* **Compile SDK**: 35 (Android 15)
* **Kotlin Version**: 2.2.10
* **Android Gradle Plugin (AGP)**: 9.2.0
* **Gradle Version**: 9.4.1 (from wrapper)
* **Java Version**: 17
* **Build Features**: Compose, BuildConfig

---

## 5. Dependencies

Major direct dependencies based on `libs.versions.toml`:

* **Jetpack Compose BOM**: `2024.12.01`
* **Room**: `2.6.1`
* **Hilt**: `2.59.2`
* **Retrofit**: `2.11.0`
* **OkHttp**: `4.12.0`
* **Coroutines**: `1.9.0`
* **Navigation Compose**: `2.8.5`
* **Lifecycle**: `2.8.7`
* **DataStore**: `1.1.1`
* **Coil**: `2.7.0`
* **Vico Charts**: `2.0.0-beta.2`
* **MaxMind GeoIP2**: `4.2.0`
* **Gson**: `2.11.0`

---

## 6. Android Manifest and Permissions

Declared in `AndroidManifest.xml`. 

| Permission | Purpose | Notes |
|---|---|---|
| `INTERNET` | General network access & VPN relay | |
| `ACCESS_NETWORK_STATE` | Monitor active connections | |
| `ACCESS_WIFI_STATE` | Check Wi-Fi properties | |
| `CHANGE_NETWORK_STATE` | Network listener triggers | |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Required for Wi-Fi scanning on older Android | |
| `NEARBY_WIFI_DEVICES` | Wi-Fi scanning (API 33+) | Flagged `neverForLocation` |
| `FOREGROUND_SERVICE` | Persist VPN Service | |
| `FOREGROUND_SERVICE_SPECIAL_USE` | VPN categorisation (API 34+) | |
| `POST_NOTIFICATIONS` | Alerts and VPN ongoing notification | |
| `QUERY_ALL_PACKAGES` | Enumerate apps for Firewall | Requires Google Play declaration |
| `WATCH_APPOPS` | Track Mic/Camera access | Requires ADB grant (`pm grant`) |
| `PACKAGE_USAGE_STATS` | Usage data for sensor logging | User grants via System Settings |
| `RECEIVE_BOOT_COMPLETED` | Auto-start VPN on boot | |
| `READ_PHONE_STATE` | IMSI catcher / cellular downgrade detection | |

* **Services**: `GateKeeperVpnService` (binds `android.net.VpnService`, handles traffic).
* **Receivers**: `BootReceiver` (boot trigger), `GateKeeperDeviceAdminReceiver` (device admin).
* **Activities**: `MainActivity` (single activity host).

---

## 7. Navigation and Screens

Defined in `ui/navigation/Screen.kt` and implemented across `ui/screens`.

| Route | UI Component | Description |
|---|---|---|
| `dashboard` | `DashboardScreen` | Main landing. Shows VPN status, summary metrics, recent alerts. |
| `protect_hub` | `ProtectHubScreen` | Sub-navigation hub for Firewall, DNS, and Threat features. |
| `alerts` | `AlertsScreen` | Dedicated list of all historical and active security alerts. |
| `settings` | `SettingsScreen` | App configuration, VPN behavior, and data management. |
| `firewall` | `FirewallScreen` | Per-app network access control, screen-off rules, schedules. |
| `dns` | `DnsFilterScreen` | Custom blocklists, whitelists, and threat subscriptions. |
| `threat_feed` | `ThreatFeedScreen` | Imported IP and Domain threat intelligence indicators. |
| `wifi_scanner` | `WifiScannerScreen` | Scans nearby networks for security risks and Evil Twins. |
| `cert_audit` | `CertAuditScreen` | Checks user CA store for known MITM proxies. |
| `permission_auditor` | `PermissionAuditorScreen` | Scans and risk-scores installed apps based on permissions. |
| `traffic` | `TrafficScreen` | Real-time `BandwidthMonitor` charts and historical connection logs. |
| `onboarding` | `OnboardingScreen` | Initial setup, permission requests, and tutorial. |

---

## 8. VPN Engine

**Core Class**: `vpn/GateKeeperVpnService.kt`

* **MTU**: `16384` (Increased to support large DNS/EDNS0 responses).
* **IP Configuration**: Virtual IP `10.120.0.1`, DNS `10.120.0.2`.
* **Routing Strategy**: Hybrid Firewall. 
    * If apps are manually blocked: Employs `Strict Firewall Mode`. Only blocked apps are routed to the tunnel (`addAllowedApplication`), their DNS is sinkholed, and TCP/UDP is blocked. Allowed apps bypass the VPN entirely for native speed.
    * If no apps are manually blocked: Employs `DNS-Only Split Tunnel`. `0.0.0.0/0` is NOT routed; only DNS traffic is routed and intercepted.
* **PCAP Capture**: Supported via `PcapWriter.kt`. Rotates captures exceeding 50MB and prunes stale files > 24h.
* **Background Sweeping**: Sweeps idle connections in `ConnectionTracker` and stale sessions in `TcpRelayHandler`/`UdpRelayHandler` every 30s.

---

## 9. Packet Filtering

**Core Class**: `vpn/PacketFilter.kt`

* **Protocol Parsing**: IPv4 parsing implemented. IPv6 parsing exists for basic headers.
* **Verdicts**: `ALLOW`, `DROP`, `DNS_INTERCEPT`, `DNS_SINKHOLE`, `DNS_LEAK`.
* **DNS-over-HTTPS (DoH)**: Hardcoded IPs (e.g. 8.8.8.8, 1.1.1.1) on port 443 trigger `DNS_LEAK` and are dropped when DoH prevention is enabled.
* **Context-Aware Rules**: 
    * Screen-off blocking sets are respected via `updateScreenOffBlockedUids`.
    * Time-schedule blocks (`packetFilter?.updateScheduledBlockedUids`) support blocking apps during specific minutes of the day.

---

## 10. DNS System

**Core Classes**: `vpn/DnsResolver.kt`, `vpn/DnsBlocklistManager.kt`

* **Interception**: Queries for blocked domains return `0.0.0.0`. Queries for allowed domains are forwarded to the system resolver using standard UDP sockets.
* **SafeSearch**: Supported and monitored via DataStore `SAFE_SEARCH_ENABLED`.
* **Exfiltration Heuristics**: `DnsBlocklistManager.checkDnsExfiltration()` detects base64 patterns, high entropy (Shannon > 3.5), and query rate anomalies (>20/min).

---

## 11. Traffic Monitoring

**Core Classes**: `vpn/TrafficLogger.kt`, `vpn/BandwidthMonitor.kt`, `vpn/ConnectionTracker.kt`

* **UID Mapping**: Uses `ConnectivityManager.getConnectionOwnerUid()` on API 29+.
* **BandwidthMonitor**: An aggregated memory buffer tracking real-time per-second speeds (bytesIn/bytesOut) specifically for rendering fast, live Vico charts in the UI.
* **Persistence**: Every 5 seconds, active connections are flushed to `ConnectionLog` Room entities.
* **GeoIP**: Local MaxMind GeoLite2 database provides offline Country resolution.

---

## 12. Security Modules

| Module | Location | Trigger | Status / Action |
|---|---|---|---|
| **ThreatFeed** | `ThreatFeedManager.kt` | Background sync | Downloads and parses blocklists. Syncs into memory. Fully Implemented. |
| **Rogue AP** | `RogueApDetector.kt` | Network availability change | Checks BSSID against Room `KnownNetwork`. Alerts on Evil Twin. Fully Implemented. |
| **Cellular Monitor** | `CellularMonitor.kt` | Connectivity changes | Checks if network downgraded to 2G (IMSI Catcher). Generates `CRITICAL` alert. Fully Implemented. |
| **Exfiltration** | `ExfiltrationDetector.kt`| Packet Loop / Outbound | Correlates large uploads with recent microphone/camera access. Fully Implemented. |
| **Cert Auditor** | `CertificateAuditor.kt` | UI Manual Scan | Scans `AndroidCAStore` for known interceptors (SuperFish, AdGuard, etc.). Fully Implemented. |
| **Privacy Logger**| `PrivacyAccessLogger.kt`| Background polling | Uses `AppOpsManager` to track mic/camera. Fully Implemented. |
| **Permission Scan**| `PermissionScanner.kt` | UI Manual Scan | Categorises apps into risk tiers based on Manifest declarations. Fully Implemented. |

---

## 13. Data Layer

### 13.1 Room Database (`gatekeeper.db`)

* **Version**: `12` (Migrations registered in `AppModule.kt` cover 2->3 up to 8->9. Remaining schema changes rely on destructive migrations or are unlisted in `addMigrations`).
* **Entities (9)**: `FirewallRule`, `DnsEntry`, `ConnectionLog`, `IpRule`, `ThreatFeedEntry`, `KnownNetwork`, `SensorLog`, `SecurityAlert`, `BlocklistSubscription`.
* **DAOs**: 9 matching DAOs provided via Hilt.

### 13.2 DataStore Preferences (`SettingsRepository.kt`)

Currently stores 14 preference keys:
* `capture_pcap`
* `backend_ip`
* `dns_leak_protection`
* `dns_exfil_detection`
* `screen_off_blocking_global`
* `imsi_detection`
* `firewall_bypass_detect`
* `background_sensor_alerts`
* `evil_twin_detection`
* `global_camera_block`
* `onboarding_done`
* `auto_vpn_start`
* `safe_search_enabled`
* `theme_mode`

---

## 14. Background Behavior

* **BootReceiver**: Checks `SettingsRepository.autoVpnStart` and starts VPN service on boot.
* **GateKeeperDeviceAdminReceiver**: Manages hardware-level lockouts (camera disabling, etc.). Requires user to manually enable Device Admin.
* **Notification Channels**: Uses `vpn_service` (ongoing), `security_alerts` (general), and `gk_security_alerts` (high priority / vibration).
* **WorkManager**: `work-runtime-ktx` is included as a dependency, but background syncs are generally managed via Coroutines in the active VPN Service or UI ViewModels, not explicit Workers.

---

## 15. Testing

**Currently, there are no automated unit or instrumented tests implemented in the repository.**

The `app/src/test` and `app/src/androidTest` directories do not exist or are empty. Verification of all features relies on manual runtime testing. 

Commands like `./gradlew test` will execute successfully but run zero tests.

---

## 16. Development Setup

1. Clone repository.
2. Ensure JDK 17 is installed.
3. Open in Android Studio (Ladybug or later).
4. Run standard Gradle Sync. Gradle Wrapper (9.4.1) will auto-download.
5. Deploy to a physical device or emulator running API 26+ (API 29+ recommended for full UID tracking).

---

## 17. Feature Implementation Matrix

| Feature | UI Available | Core Logic | Status | Notes |
|---|:---:|:---:|---|---|
| VPN Tunnel & Relay | N/A | ✅ | Fully Implemented | Includes TCP/UDP relays |
| App Firewall | ✅ | ✅ | Fully Implemented | Includes screen-off and time scheduling |
| DNS Sinkhole | ✅ | ✅ | Fully Implemented | In-memory sync is live |
| Traffic Monitor | ✅ | ✅ | Fully Implemented | Uses Vico charts and MaxMind GeoIP |
| PCAP Capture | ✅ | ✅ | Fully Implemented | Rotates at 50MB |
| Evil Twin Detect | ✅ | ✅ | Fully Implemented | Triggers on Network Callback |
| IMSI Catcher Detect| ✅ | ✅ | Fully Implemented | Tracks 2G downgrades |
| Cert & Perm Auditor| ✅ | ✅ | Fully Implemented | Works offline |
| SafeSearch | ✅ | ✅ | Fully Implemented | Modifies DNS payloads |

---

## 18. Known Limitations

* **UID Resolution (Emulator)**: On API < 29, `ConnectivityManager.getConnectionOwnerUid()` is unavailable. Traffic logging may map everything to `Unknown App`.
* **Private DNS / DoH**: If an app uses hardcoded DoH IPs that aren't in the internal drop list, it will successfully bypass the DNS sinkhole.
* **IPv6 Parsing**: IPv6 packet parsing is functional but less robust than the IPv4 implementation.
* **Hardware Dependencies**: Wi-Fi Scanning and BSSID Evil Twin detection require `ACCESS_FINE_LOCATION` and active Location Services on the device.
* **Automated Tests**: Lack of unit tests means regressions must be caught manually during QA.
* **ADB Permissions**: Features like `PrivacyAccessLogger` require manual ADB commands (`pm grant ... android.permission.WATCH_APPOPS`) to function properly.

---

## 19. Supervisor Demonstration Notes

* **Safe to demonstrate live**: App Firewall (blocking Chrome/YouTube), DNS Filter (visiting an ad-tester site), Traffic Monitor (watching live bandwidth spike), Permission Auditor.
* **Requires prepared data**: Evil Twin and IMSI Catcher detection. (Requires modifying the DB or spoofing BSSIDs/network states).
* **Requires ADB**: Do not forget to run the `WATCH_APPOPS` grant command before demonstrating Exfiltration Detection or the Sensor Logger, otherwise it will fail silently.
