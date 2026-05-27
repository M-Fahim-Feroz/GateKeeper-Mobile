# GateKeeper Mobile — Comprehensive Project Documentation

<div align="center">
  <h3>🛡️ AI-Powered Android Network Security Suite</h3>
  <p>Built as part of an Advanced Network Security Final Year Project</p>
</div>

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
   - [High-Level Design](#21-high-level-design)
   - [Package Structure](#22-package-structure)
   - [Data Flow](#23-data-flow)
3. [Core Engine — VPN Service](#3-core-engine--vpn-service)
   - [GateKeeperVpnService](#31-gatekeepervpnservice)
   - [PacketFilter](#32-packetfilter)
   - [DnsResolver & DnsBlocklistManager](#33-dnsresolver--dnsblocklistmanager)
   - [ConnectionTracker](#34-connectiontracker)
   - [TrafficLogger](#35-trafficlogger)
4. [Security Modules](#4-security-modules)
   - [ThreatFeedManager](#41-threatfeedmanager)
   - [RogueApDetector (Evil Twin Wi-Fi)](#42-rogueapdetector-evil-twin-wi-fi)
   - [CellularMonitor (IMSI Catcher Detection)](#43-cellularmonitor-imsi-catcher-detection)
   - [ExfiltrationDetector](#44-exfiltrationdetector)
   - [CertificateAuditor](#45-certificateauditor)
   - [PrivacyAccessLogger](#46-privacyaccesslogger)
   - [PermissionScanner](#47-permissionscanner)
5. [Data Layer](#5-data-layer)
   - [Room Database Schema](#51-room-database-schema)
   - [Repositories](#52-repositories)
   - [DataStore Preferences](#53-datastore-preferences)
   - [Remote API (AI Backend)](#54-remote-api-ai-backend)
6. [UI Layer — Screens & ViewModels](#6-ui-layer--screens--viewmodels)
   - [Dashboard](#61-dashboard)
   - [Firewall](#62-firewall)
   - [DNS Filter](#63-dns-filter)
   - [Traffic Monitor](#64-traffic-monitor)
   - [AI Chat](#65-ai-chat)
   - [Threat Feed](#66-threat-feed)
   - [Wi-Fi Scanner](#67-wi-fi-scanner)
   - [Permission Auditor](#68-permission-auditor)
   - [Certificate Auditor](#69-certificate-auditor)
   - [Settings](#610-settings)
   - [Onboarding](#611-onboarding)
7. [Navigation](#7-navigation)
8. [Dependency Injection (Hilt)](#8-dependency-injection-hilt)
9. [Background Components](#9-background-components)
   - [Boot Receiver](#91-boot-receiver)
   - [Device Admin Receiver](#92-device-admin-receiver)
   - [Notification Manager](#93-notification-manager)
10. [Built-In DNS Blocklists](#10-built-in-dns-blocklists)
11. [Utilities](#11-utilities)
12. [Permissions Reference](#12-permissions-reference)
13. [Build Configuration](#13-build-configuration)
14. [Database Migrations](#14-database-migrations)
15. [Ecosystem Integration](#15-ecosystem-integration)
16. [Development Setup](#16-development-setup)
17. [Testing](#17-testing)
18. [Security Features Quick-Reference](#18-security-features-quick-reference)

---

## 1. Project Overview

**GateKeeper Mobile** is a rootless Android security application that intercepts and inspects all device network traffic using Android's `VpnService` API. It provides system-wide protection without requiring root access, combining:

| Capability | Description |
|---|---|
| Per-App Firewall | Block specific apps from using Wi-Fi or Mobile Data |
| DNS Sinkhole | Block ad, tracking, and malware domains at the DNS layer |
| Traffic Monitor | Real-time per-app bandwidth and connection logging |
| AI Security Assistant | Natural language firewall control via GateKeeper-Agent backend |
| Threat Intelligence | IP & domain blacklists from public threat feeds |
| Evil Twin Detection | Rogue Wi-Fi access point detection via BSSID tracking |
| IMSI Catcher Detection | Alerts on suspicious cellular downgrade to 2G |
| Data Exfiltration Detection | Correlates large uploads with recent sensor (mic/camera) use |
| Certificate Auditor | Scans user-installed CA certificates for known MITM proxies |
| Permission Auditor | Risk-scores installed apps based on declared permissions |

**Platform**: Android 8.0+ (API 26+)  
**Language**: Kotlin 2.1  
**UI**: Jetpack Compose with Material 3  
**Architecture**: Clean Architecture + MVVM

---

## 2. Architecture

### 2.1 High-Level Design

```
┌────────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                      │
│  Dashboard │ Firewall │ DNS │ Traffic │ AI │ Settings │ … │
└──────────────────────┬─────────────────────────────────────┘
                       │ ViewModel (StateFlow / Flow)
┌──────────────────────▼─────────────────────────────────────┐
│                  Domain Layer (Use Cases)                    │
│  GetInstalledAppsUseCase │ ScanWifiNetworksUseCase │ …      │
└──────────────────────┬─────────────────────────────────────┘
                       │ Repository interfaces
┌──────────────────────▼─────────────────────────────────────┐
│                   Data Layer                                 │
│  Repositories ─► Room DB  │  Repositories ─► Retrofit API  │
│                 DataStore  │          GeoIP (MaxMind)       │
└──────────────────────┬─────────────────────────────────────┘
                       │ Inject into VPN service
┌──────────────────────▼─────────────────────────────────────┐
│             Core VPN Engine (GateKeeperVpnService)          │
│  TUN Interface ─► PacketFilter ─► DnsResolver               │
│                ─► ConnectionTracker ─► TrafficLogger        │
│                ─► ThreatFeedManager                        │
└────────────────────────────────────────────────────────────┘
```

The VPN service and UI layer both run in the same process; the service exposes static `StateFlow` objects that the Compose UI observes directly for live updates.

### 2.2 Package Structure

```
com.gatekeeper.mobile/
│
├── GateKeeperApp.kt              # Application class — Hilt entry point, notification channels
│
├── vpn/                          # Core VPN engine
│   ├── GateKeeperVpnService.kt   # TUN interface, packet loop, orchestration
│   ├── PacketFilter.kt           # Stateless packet inspection (IPv4/IPv6)
│   ├── DnsResolver.kt            # DNS query parsing & response crafting
│   ├── DnsBlocklistManager.kt    # In-memory blocklist, DNS exfiltration detection
│   ├── ConnectionTracker.kt      # 5-tuple connection tracking, UID mapping
│   ├── TrafficLogger.kt          # Per-app bandwidth aggregation → Room
│   ├── ThreatFeedManager.kt      # Downloads & imports IP/domain threat feeds
│   ├── RogueApDetector.kt        # Evil Twin Wi-Fi detection
│   ├── CellularMonitor.kt        # IMSI catcher / 2G downgrade detection
│   ├── ExfiltrationDetector.kt   # Correlates sensor use + large uploads
│   ├── CertificateAuditor.kt     # Scans AndroidCAStore for risky user certs
│   ├── PrivacyAccessLogger.kt    # Tracks mic/camera sensor access
│   ├── PermissionScanner.kt      # Risk-scores app permissions
│   ├── WifiScanner.kt            # Reads current Wi-Fi connection info
│   ├── GeoIpResolver.kt          # MaxMind GeoLite2 country lookup
│   ├── NetworkUtils.kt           # Low-level IP byte utilities
│   ├── PcapWriter.kt             # PCAP capture file writer (F18)
│   └── ExportUtils.kt            # CSV/JSON log export helpers
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt        # Room database definition (v8)
│   │   ├── dao/                  # Data Access Objects (9 DAOs)
│   │   └── entity/               # Room entities (9 tables)
│   ├── model/
│   │   └── BuiltInBlocklists.kt  # Hardcoded curated blocklist URLs
│   ├── remote/
│   │   ├── AiApiService.kt       # Retrofit interface for GateKeeper-Agent
│   │   └── dto/Dtos.kt           # Request/Response data classes
│   └── repository/               # Repository implementations (8 repositories)
│
├── domain/
│   ├── model/Models.kt           # Clean domain models (InstalledApp, ChatMessage, …)
│   └── usecase/                  # Use case classes (3)
│
├── di/
│   └── AppModule.kt              # Hilt @Module for DB, Retrofit, DAOs
│
├── notifications/
│   └── GKNotificationManager.kt  # Notification helpers for VPN & alerts
│
├── receiver/
│   ├── BootReceiver.kt           # Auto-start VPN on device boot
│   └── GateKeeperDeviceAdminReceiver.kt  # Device admin for hardware-level controls
│
└── ui/
    ├── MainActivity.kt           # Single-activity host
    ├── navigation/
    │   ├── Screen.kt             # Sealed class of all navigation destinations
    │   └── AppNavigation.kt      # NavHost & bottom navigation setup
    ├── screens/                  # Feature screens (Screen + ViewModel pairs)
    │   ├── dashboard/
    │   ├── firewall/
    │   ├── dns/
    │   ├── traffic/
    │   ├── aichat/
    │   ├── threats/
    │   ├── wifiscanner/
    │   ├── permissionauditor/
    │   ├── certaudit/
    │   ├── settings/
    │   └── onboarding/
    ├── components/               # Reusable Compose components
    │   ├── Components.kt
    │   └── GKComponents.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        ├── Typography.kt
        └── Glassmorphism.kt
```

### 2.3 Data Flow

#### Packet Lifecycle (Outbound)

```
App sends IP packet
      │
      ▼
TUN interface fd (read loop in GateKeeperVpnService)
      │
      ▼
ConnectionTracker.track()  ──► UID mapping via ConnectivityManager (API 29+)
      │
      ▼
PacketFilter.filter()
      ├── PacketVerdict.DROP          → packet silently discarded
      ├── PacketVerdict.DNS_SINKHOLE  → immediate 0.0.0.0 response synthesised
      ├── PacketVerdict.DNS_INTERCEPT → DnsResolver.handleQuery()
      │       ├── domain blocked?  → NXDOMAIN / 0.0.0.0 response
      │       └── domain allowed?  → forward to system DNS, log result
      ├── PacketVerdict.DNS_LEAK      → packet dropped (DoH bypass)
      └── PacketVerdict.ALLOW        → forward via protect()'ed socket
                                         │
                                         ▼
                                     TrafficLogger.log()
                                     ExfiltrationDetector.analyzeTraffic()
```

---

## 3. Core Engine — VPN Service

### 3.1 GateKeeperVpnService

**File**: `vpn/GateKeeperVpnService.kt`  
**Base class**: `android.net.VpnService`  
**Injection**: `@AndroidEntryPoint` (Hilt)

The service is the central orchestrator of all real-time security functions.

#### Lifecycle

| Phase | What Happens |
|---|---|
| `onStartCommand(ACTION_START)` | Requests VPN permission, creates TUN interface, starts packet loop coroutine, registers screen-state broadcast receiver, starts cellular monitor, connects Wi-Fi rogue AP checker |
| `onStartCommand(ACTION_STOP)` | Cancels all coroutines, closes TUN fd, unregisters receivers |
| `onRevoke()` | Called by Android if user revokes VPN permission; stops cleanly |

#### TUN Interface Setup

The service builds a `VpnService.Builder` that:
- Assigns the virtual IP `10.0.0.2/32` to the device.
- Routes all traffic (`0.0.0.0/0`, `::/0`) through the tunnel.
- Sets the DNS server to `10.0.0.1` so all DNS queries are intercepted locally.
- Uses an MTU of 32767.
- Excludes its own package from the VPN to avoid routing loops.

#### Packet Loop

Runs on a `Dispatchers.IO` coroutine. Each iteration:
1. Reads one IP packet from the TUN `FileInputStream` into a `ByteBuffer`.
2. Calls `ConnectionTracker.track()` to identify the owning UID.
3. Calls `PacketFilter.filter()` to get a verdict.
4. Acts on the verdict (drop, sinkhole, intercept DNS, or forward).
5. For forwarded packets, writes to the upstream `FileOutputStream`.

#### Static State Flows (observed by UI)

| Property | Type | Description |
|---|---|---|
| `isRunning` | `StateFlow<Boolean>` | Whether the VPN tunnel is active |
| `blockedCount` | `StateFlow<Long>` | Running total of blocked packets |
| `dnsBlockedCount` | `StateFlow<Long>` | Running total of sinkhol'd DNS queries |

#### Supported Actions (Intent)

| Action constant | Effect |
|---|---|
| `ACTION_START` | Start the VPN |
| `ACTION_STOP` | Stop the VPN |
| `ACTION_REBUILD` | Rebuild the TUN interface (re-apply updated firewall rules) |
| `ACTION_START_PCAP` | Begin PCAP capture to file |
| `ACTION_STOP_PCAP` | Stop PCAP capture |

---

### 3.2 PacketFilter

**File**: `vpn/PacketFilter.kt`

A stateless (except for its rule sets) packet inspector. Runs on the packet-loop thread, so all operations must be non-blocking.

#### Verdict Enum

| Verdict | Meaning |
|---|---|
| `ALLOW` | Forward packet normally |
| `DROP` | Silently discard |
| `DNS_INTERCEPT` | DNS query from allowed app — handle locally |
| `DNS_SINKHOLE` | DNS query from blocked app — return 0.0.0.0 instantly |
| `DNS_LEAK` | App is connecting to a DoH provider on port 443 — drop |

#### Rule Sets (mutable, updated atomically)

| Method | Description |
|---|---|
| `updateBlockedUids(Set<Int>)` | Replace the set of blocked app UIDs |
| `updateBlockedIps(Set<String>)` | Replace the set of blocked destination IPs |
| `updateBlockedPorts(Set<Int>)` | Replace the set of blocked destination ports |
| `updateScreenOffBlockedUids(Set<Int>)` | UIDs that are additionally blocked when the screen is off |

#### Protocol Support

- **IPv4**: Full parsing of IHL, protocol, source/destination IPs, source/destination ports for TCP and UDP.
- **IPv6**: Fixed 40-byte header parsing with next-header field; supports TCP (6) and UDP (17).

#### DNS-over-HTTPS (DoH) Bypass Detection

Connections to TCP port 443 targeting well-known DoH resolvers (Google `8.8.8.8`, Cloudflare `1.1.1.1`, Quad9 `9.9.9.9`, OpenDNS) return `DNS_LEAK` and are dropped when `blockDnsLeak` is `true`.

---

### 3.3 DnsResolver & DnsBlocklistManager

**Files**: `vpn/DnsResolver.kt`, `vpn/DnsBlocklistManager.kt`

#### DnsResolver

Parses raw DNS UDP payloads and synthesises responses:
- **Allowed domain**: Forwards query to the system's upstream DNS server, returns the real answer, and registers resolved IPs in `DnsBlocklistManager.recentDnsResolutions` for bypass detection.
- **Blocked domain**: Returns a crafted DNS response with `0.0.0.0` (IPv4) or `::` (IPv6), preventing the OS from caching a real IP and bypassing the filter.

#### DnsBlocklistManager

Manages three in-memory concurrent structures:

| Structure | Purpose |
|---|---|
| `blacklistedDomains` (`ConcurrentHashSet`) | Blocked domains (user rules + threat feed domains) |
| `whitelistedDomains` (`ConcurrentHashSet`) | Explicitly allowed domains (take priority over blacklist) |
| `recentDnsResolutions` (`ConcurrentHashMap<IP, timestamp>`) | Recently resolved IPs for hardcoded-IP bypass detection (F14) |

**Live sync**: `observeAndSync()` is called from within the VPN service coroutine scope. It permanently collects two Room `Flow` streams (blacklist, whitelist) so that any user rule change in the UI is reflected in the VPN within milliseconds without a restart.

**Domain matching**: Both exact match and suffix match are supported — blocking `facebook.com` also blocks `api.facebook.com`, `static.xx.fbcdn.net`, etc.

**DNS Exfiltration Detection** (`checkDnsExfiltration()`):
Checks every resolved domain for three heuristic signals:
1. Subdomain length > 40 characters (likely base64-encoded stolen data).
2. Shannon entropy > 3.5 with subdomain length > 10 (randomly-looking subdomain = DNS tunnel).
3. Query rate > 20/minute to the same base domain (DNS tunnelling activity).

Triggers a `SecurityAlert` of type `DNS_EXFILTRATION` with severity `HIGH`.

**Blocklist import** (`importFromUrl()`): Downloads a hosts-file or plain-domain-list from any URL, parses it (handles `0.0.0.0 domain` and `127.0.0.1 domain` formats), batch-inserts up to 5,000 domains at a time into Room, then reloads the in-memory set.

---

### 3.4 ConnectionTracker

**File**: `vpn/ConnectionTracker.kt`

Maintains a live map of active network connections keyed by 5-tuple (`protocol:srcIP:srcPort-dstIP:dstPort`).

#### UID Mapping

On Android 10+ (API 29), `ConnectivityManager.getConnectionOwnerUid()` maps a TCP/UDP socket to the Android UID of the owning app. UID→app name lookups are cached in `uidAppCache` to avoid repeated `PackageManager` calls on the hot path.

#### Idle Connection Cleanup

`sweepIdle(idleThresholdMs)` is called every 5 seconds from the reporting loop to remove connections with zero data transfer older than the threshold (default 5 minutes).

#### Country Classification

`classifyIpRegion()` performs a fast local classification (RFC 1918 private → "Local Network", everything else → "Internet") without any network call to avoid ANR risks. Full GeoIP country lookup is delegated to `GeoIpResolver`.

---

### 3.5 TrafficLogger

**File**: `vpn/TrafficLogger.kt`

Aggregates per-app bandwidth statistics and periodically flushes them to `ConnectionLog` Room entities. It:
- Accumulates `bytesIn` / `bytesOut` per (packageName, remoteIp) tuple.
- Invokes `ExfiltrationDetector.analyzeTraffic()` for every recorded outbound flow.
- Performs GeoIP country lookup via `GeoIpResolver` for each new destination IP.

---

## 4. Security Modules

### 4.1 ThreatFeedManager

**File**: `vpn/ThreatFeedManager.kt`

Downloads and imports threat intelligence feeds from public URLs. Each feed entry is stored as a `ThreatFeedEntry` in Room with the indicator (IP or domain), indicator type, feed source URL, feed name, and threat type. Batch inserts of 5,000 entries prevent memory pressure. On completion, `DnsBlocklistManager.reloadAll()` is triggered to include the new threat domains in the active blocklist.

---

### 4.2 RogueApDetector (Evil Twin Wi-Fi)

**File**: `vpn/RogueApDetector.kt`

Detects evil twin access points by comparing the current Wi-Fi BSSID (router hardware address) against previously seen BSSIDs for the same SSID stored in the `known_networks` Room table.

| Result | Condition | Action |
|---|---|---|
| `SAFE` | SSID never seen before (and secured) OR BSSID is known | None |
| `OPEN_NETWORK` | New SSID with no password (WEP/OPEN) | Warning level alert |
| `EVIL_TWIN` | Known SSID but BSSID is unknown | `CRITICAL` SecurityAlert + push notification |

Every successful connection updates/inserts the network record in Room so the trusted BSSID set grows over time.

---

### 4.3 CellularMonitor (IMSI Catcher Detection)

**File**: `vpn/CellularMonitor.kt`

Registers a `TelephonyCallback` (API 31+) or `PhoneStateListener` (API 26-30) to listen for data connection type changes. When the network degrades from any higher generation to **2G**, it fires a `CRITICAL` security alert titled *"Cellular Downgrade to 2G Detected"* explaining the IMSI catcher / Stingray threat model.

Alert de-duplication: `alertedForDowngrade` flag prevents repeated alerts for the same 2G episode. The flag resets when the network upgrades back to 3G/4G/5G.

---

### 4.4 ExfiltrationDetector

**File**: `vpn/ExfiltrationDetector.kt`

Correlates two asynchronous data streams to detect real-time data exfiltration:
1. Sensor access logs from `SensorLogRepository` (microphone, camera activations logged by `PrivacyAccessLogger`).
2. Large outbound uploads (>500 KB) detected by `TrafficLogger`.

If an app sent a large upload to an external IP within 2 minutes of accessing the microphone or camera, a `CRITICAL` alert *"Potential Data Exfiltration"* is raised. A per-`(packageName, remoteIp)` dedup set prevents alert spam.

---

### 4.5 CertificateAuditor

**File**: `vpn/CertificateAuditor.kt`

Enumerates user-installed CA certificates from the `AndroidCAStore` KeyStore (aliases beginning with `user:`). For each certificate it:
1. Checks the issuer Distinguished Name against a known-risky issuer list (SuperFish, Komodia, Charles Proxy, Fiddler, mitmproxy, BurpSuite, AdGuard, HttpCanary, etc.).
2. Checks if the certificate has expired.
3. Assigns a risk level: `HIGH` (known MITM proxy CA), `MEDIUM` (expired or suspicious), `LOW` (unknown user cert).

Returns a list of `RogueCertInfo` records. High-risk certificates generate a `SecurityAlert` and a push notification.

---

### 4.6 PrivacyAccessLogger

**File**: `vpn/PrivacyAccessLogger.kt`

Uses `AppOpsManager` to detect which apps are currently using sensitive sensors (microphone, camera). Logged to `SensorLog` Room entities including package name, sensor type, start time, duration, and whether the app was in the background at the time.

---

### 4.7 PermissionScanner

**File**: `vpn/PermissionScanner.kt`

Enumerates all installed apps from `PackageManager` and scores their declared permissions using a tiered risk taxonomy:

| Tier | Examples | Weight |
|---|---|---|
| Critical surveillance | `READ_CONTACTS`, `RECORD_AUDIO`, `CAMERA`, `ACCESS_FINE_LOCATION`, `READ_CALL_LOG`, `READ_SMS` | High |
| High sensitivity | `PROCESS_OUTGOING_CALLS`, `RECEIVE_SMS`, `READ_MEDIA_*` | Medium-High |
| Low sensitivity | `VIBRATE`, `SET_ALARM` | Low |

The cumulative risk score (0–100) and tier (CRITICAL / HIGH / MEDIUM / LOW) are returned as `AppPermissionInfo` domain objects.

---

## 5. Data Layer

### 5.1 Room Database Schema

**Database name**: `gatekeeper.db`  
**Current version**: 8  
**File**: `data/db/AppDatabase.kt`

#### Tables

| Entity | Table | Purpose |
|---|---|---|
| `FirewallRule` | `firewall_rules` | Per-app firewall rules (UID, packageName, isBlocked, blockWhenScreenOff) |
| `DnsEntry` | `dns_entries` | DNS blocklist/allowlist entries (domain, type, source, isActive) |
| `ConnectionLog` | `connection_logs` | Per-connection traffic records (app, IP, bytes, country, timestamp, isSystemEvent) |
| `IpRule` | `ip_rules` | Manual IP block/allow rules |
| `ThreatFeedEntry` | `threat_feed_entries` | Imported threat intelligence indicators (IP or domain) |
| `KnownNetwork` | `known_networks` | Trusted Wi-Fi networks indexed by SSID+BSSID |
| `SensorLog` | `sensor_logs` | Sensor access records (mic/camera, package, background flag) |
| `SecurityAlert` | `security_alerts` | Security events with type, severity, title, description, resolved flag |
| `BlocklistSubscription` | `blocklist_subscriptions` | User-subscribed external blocklist feeds (URL, name, type, enabled, lastRefreshedAt) |

#### Key DAOs

| DAO | Key Operations |
|---|---|
| `FirewallRuleDao` | Insert/update rule, query blocked UIDs as Flow, count blocked apps |
| `DnsBlocklistDao` | Insert domains, query active black/whitelist as Flow, delete by source |
| `ConnectionLogDao` | Insert log, query recent logs as Flow, total count |
| `IpRuleDao` | Insert/delete IP rules, query as Flow |
| `ThreatFeedDao` | Bulk insert feed entries, query all threat domains, delete by source |
| `KnownNetworkDao` | Get known BSSIDs for SSID, upsert network record |
| `SensorLogDao` | Insert log, observe recent entries as Flow |
| `SecurityAlertDao` | Insert alert, observe unresolved/all alerts as Flow, mark resolved |
| `BlocklistSubscriptionDao` | CRUD for subscriptions |

---

### 5.2 Repositories

All repositories are `@Singleton` injected via Hilt. They act as the single source of truth between the VPN engine, ViewModels, and Room.

| Repository | Responsibility |
|---|---|
| `FirewallRepository` | Manage firewall rules; provide blocked UIDs and screen-off UIDs as `Flow` |
| `DnsRepository` | Manage DNS black/whitelist entries and subscriptions |
| `TrafficRepository` | Read/write `ConnectionLog`; provide live traffic `Flow` |
| `ThreatFeedRepository` | Import and query threat feed entries; provide domain set for `DnsBlocklistManager` |
| `SecurityAlertRepository` | Create and resolve security alerts |
| `KnownNetworkRepository` | SSID/BSSID trust tracking |
| `SensorLogRepository` | Store and observe sensor access records |
| `SettingsRepository` | Persist user preferences via Jetpack DataStore |
| `AiChatRepository` | Send messages to and receive responses from the AI backend |

---

### 5.3 DataStore Preferences

**Repository**: `SettingsRepository`

Preferences stored via Jetpack DataStore (no XML SharedPreferences):

| Key | Type | Default | Description |
|---|---|---|---|
| `backend_ip` | String | `""` | IP address of the GateKeeper-Agent server |
| `vpn_auto_start` | Boolean | `false` | Auto-start VPN on boot |
| `block_dns_leak` | Boolean | `true` | Drop DNS-over-HTTPS bypass attempts |
| `screen_off_blocking` | Boolean | `false` | Enable global screen-off mode |
| `onboarding_done` | Boolean | `false` | Whether the user has completed onboarding |

---

### 5.4 Remote API (AI Backend)

**Interface**: `data/remote/AiApiService.kt`  
**Base URL**: Configurable via Settings; defaults to `http://10.0.2.2:8888/` (Android emulator loopback).

| Endpoint | Method | Description |
|---|---|---|
| `GET /health` | `checkHealth()` | Returns `{"status": "ok"}` to verify backend connectivity |
| `POST /chat` | `sendMessage(ChatRequest)` | Sends a natural language message; returns AI response with optional tool call results |
| `POST /clear-history` | `clearHistory(sessionId)` | Clears the conversation history for a session |

#### ChatResponse Fields

| Field | Type | Description |
|---|---|---|
| `response` | String | AI-generated text reply |
| `tool_calls` | List\<String\> | Names of tools the AI invoked (e.g. "block_app") |
| `execution_steps` | List\<Map\> | Step-by-step execution trace |
| `operation_results` | List\<Map\> | Outcomes of tool operations |
| `partial_failures` | List\<Map\> | Any operations that failed |
| `warnings` | List\<String\> | Non-fatal warnings |
| `outcome` | String | Summary outcome (e.g. "success") |
| `trace_id` | String | Unique ID for the request (for debugging) |

---

## 6. UI Layer — Screens & ViewModels

The app uses a single-activity architecture (`MainActivity`) with Jetpack Compose Navigation. Every screen is a `@Composable` function paired with a `@HiltViewModel`.

---

### 6.1 Dashboard

**Files**: `ui/screens/dashboard/DashboardScreen.kt`, `DashboardViewModel.kt`

The main landing screen. Shows:
- **VPN Status Card** with animated shield icon, toggle button to start/stop VPN, and live blocked-packet counter.
- **Security Summary Tiles**: apps protected, DNS domains blocked, active threats, rogue certificates, unresolved security alerts.
- **Recent Alerts List**: last N `SecurityAlert` entries with severity color coding and mark-as-resolved actions.
- **Recent Traffic Preview**: last 3 connection log entries.
- **Background Sensor Access Counter**: number of mic/camera accesses in the last 24 hours while the app was in the background.
- **Quick Nav buttons**: navigate to Threat Feed, Wi-Fi Scanner, Permission Auditor, Cert Auditor.

**ViewModel observes**:
- `SecurityAlertRepository.observeUnresolved()` — unresolved alert list
- `FirewallRepository.observeBlockedCount()` — blocked app count
- `DnsRepository.observeBlacklistCount()` — blocked domain count
- `ThreatFeedRepository.observeCount()` — threat indicator count
- `TrafficRepository.observeRecent(3)` — recent connections
- `SensorLogRepository.observeRecent()` — recent sensor access
- `CertificateAuditor.auditUserCertificates()` — rogue cert count (refreshed on demand)
- `GateKeeperVpnService.isRunning` / `blockedCount` — static StateFlows

---

### 6.2 Firewall

**Files**: `ui/screens/firewall/FirewallScreen.kt`, `FirewallViewModel.kt`

Shows a searchable, scrollable list of all installed apps. For each app:
- App icon, name, and package name.
- Toggle to block/allow network access.
- Toggle for "block when screen off" mode (requires app to be blocked first).
- Risk badge showing the count of sensitive permissions.

**ViewModel**:
- Loads installed apps via `GetInstalledAppsUseCase` (merges `PackageManager` list with Room firewall rules).
- Calls `FirewallRepository.setBlocked(uid, packageName, isBlocked)` on toggle.
- Calls `FirewallRepository.setScreenOffBlocking(uid, isBlocked)` for screen-off mode.
- Emits the blocked UIDs set to the VPN service which calls `PacketFilter.updateBlockedUids()`.

---

### 6.3 DNS Filter

**Files**: `ui/screens/dns/DnsFilterScreen.kt`, `DnsFilterViewModel.kt`

Provides:
- **Blocklist tab**: list of blocked domains, add/remove individual domains, search filter.
- **Allowlist tab**: list of whitelisted domains.
- **Subscriptions tab**: built-in curated blocklist feeds (`BuiltInBlocklists.feeds`) and user-added feeds; subscribe/unsubscribe with one tap triggers `DnsBlocklistManager.importFromUrl()`.

The screen shows live blocked-domain count and last-refresh timestamps per subscription.

---

### 6.4 Traffic Monitor

**Files**: `ui/screens/traffic/TrafficScreen.kt`, `TrafficViewModel.kt`

Displays:
- **Live bandwidth chart** using Vico Charts showing per-second bytes in/out.
- **Connection log list** with per-entry details: app name, remote IP, remote hostname (reverse-DNS or from DNS intercept), country flag + code, protocol, bytes transferred.
- **Filter controls**: filter by app name, protocol, or country.
- **Export button**: triggers `ExportUtils` to export logs as CSV.

---

### 6.5 AI Chat

**Files**: `ui/screens/aichat/AiChatScreen.kt`, `AiChatViewModel.kt`

A conversational interface to the GateKeeper-Agent AI backend. Features:
- Chat bubble UI distinguishing user and assistant messages.
- Loading indicator while waiting for the AI response.
- Tool call badges: when the AI invoked a backend tool (e.g., `block_app`, `show_threats`), the tool names are displayed inline.
- Session management: "New Chat" clears history on the backend via `POST /clear-history`.
- Backend connectivity check on screen open via `GET /health`.

**ViewModel** uses `AiChatRepository.sendMessage()` which calls `AiApiService.sendMessage()` with the `mobile-session` session ID.

---

### 6.6 Threat Feed

**Files**: `ui/screens/threats/ThreatFeedScreen.kt`, `ThreatFeedViewModel.kt`

Shows all imported threat intelligence entries. Each entry has:
- Indicator value (IP or domain).
- Indicator type and threat type.
- Feed source name and URL.
- Import timestamp.

Supports importing new feeds by URL and clearing all feeds. Feeds update the `DnsBlocklistManager` in-memory set on completion.

---

### 6.7 Wi-Fi Scanner

**Files**: `ui/screens/wifiscanner/WifiScannerScreen.kt`, `WifiScannerViewModel.kt`

Uses `ScanWifiNetworksUseCase` which calls `android.net.wifi.WifiManager.startScan()` and maps `ScanResult` objects to `WifiNetworkInfo` domain models, computing:
- Signal level (0–4 bars via `WifiManager.calculateSignalLevel`).
- Security type (OPEN, WEP, WPA2, WPA3).
- Security score (0–100).
- Risk level ("safe", "warning", "danger").
- Vendor OUI lookup.
- Evil Twin flag (via `RogueApDetector` logic).

Displays scan results as cards with color-coded risk levels and a "Trust Network" action.

---

### 6.8 Permission Auditor

**Files**: `ui/screens/permissionauditor/PermissionAuditorScreen.kt`, `PermissionAuditorViewModel.kt`, `PrivacyDashboardChart.kt`

Runs `ScanAppPermissionsUseCase` to produce `AppPermissionInfo` objects for all installed apps. UI shows:
- Pie/bar chart (via Vico) of apps per risk tier.
- Sortable list by risk score.
- Per-app detail: list of dangerous permissions, risk score badge.

---

### 6.9 Certificate Auditor

**Files**: `ui/screens/certaudit/CertAuditScreen.kt`, `CertAuditViewModel.kt`

Invokes `CertificateAuditor.auditUserCertificates()` and displays a list of `RogueCertInfo` objects. Each card shows:
- Certificate alias, issuer, and subject DN.
- Expiry date.
- Risk level badge (`HIGH` in red, `MEDIUM` in orange, `LOW` in yellow).

A scan button re-runs the audit on demand.

---

### 6.10 Settings

**Files**: `ui/screens/settings/SettingsScreen.kt`, `SettingsLandingScreen.kt`, `SettingsSubPages.kt`, `SettingsViewModel.kt`

The settings area is split into a landing screen (list of setting categories) and sub-page composables:

| Category | Settings |
|---|---|
| VPN & Network | Auto-start on boot, DNS leak blocking |
| AI Backend | Backend IP address, port, connection test |
| Blocklists | Subscription management (also accessible from DNS screen) |
| Privacy | Screen-off blocking mode |
| About | App version, open source licenses |

All changes are persisted via `SettingsRepository` (DataStore) and propagated to the VPN service on next start or via service actions.

---

### 6.11 Onboarding

**Files**: `ui/screens/onboarding/OnboardingScreen.kt`, `OnboardingViewModel.kt`

Multi-page intro shown on first launch. Steps through:
1. Welcome and app overview.
2. VPN permission explanation and request.
3. Notification permission request.
4. Quick-start guide.

On completion, sets `onboarding_done = true` in DataStore and navigates to Dashboard.

---

## 7. Navigation

**Files**: `ui/navigation/Screen.kt`, `ui/navigation/AppNavigation.kt`

Sealed class `Screen` defines all navigation routes:

| Route | Class | Bottom Nav? |
|---|---|---|
| `dashboard` | `Screen.Dashboard` | ✅ |
| `firewall` | `Screen.Firewall` | ✅ |
| `dns` | `Screen.DnsFilter` | ✅ |
| `traffic` | `Screen.Traffic` | ✅ |
| `ai_chat` | `Screen.AiChat` | ✅ |
| `settings` | `Screen.Settings` | ✅ (in nav rail) |
| `threat_feed` | `Screen.ThreatFeed` | ❌ |
| `permission_auditor` | `Screen.PermissionAuditor` | ❌ |
| `wifi_scanner` | `Screen.WifiScanner` | ❌ |
| `cert_audit` | `Screen.CertAudit` | ❌ |

`AppNavigation.kt` sets up the `NavHost` and renders the `NavigationBar` (bottom bar) for the 5 primary destinations.

---

## 8. Dependency Injection (Hilt)

**Module file**: `di/AppModule.kt`  
**Scope**: `SingletonComponent` — all provided objects are application-scoped singletons.

### Provided Bindings

| Binding | Type | Notes |
|---|---|---|
| `AppDatabase` | Room Database | Built with 6 migrations (v2→8) |
| All 9 DAOs | DAO interfaces | Derived from `AppDatabase` |
| `OkHttpClient` | HTTP client | Intercepts requests to rewrite base URL from DataStore `backend_ip` |
| `Retrofit` | REST client | Default base `http://10.0.2.2:8888/`; overridden per-request by OkHttp interceptor |
| `AiApiService` | Retrofit interface | Created from the `Retrofit` instance |

### Dynamic Backend URL

The OkHttp interceptor reads `SettingsRepository.backendIpFlow` synchronously (via `runBlocking`) for every HTTP request and rewrites the URL to target the user-configured IP on port 8888. This allows changing the AI backend address at runtime without restarting the app.

---

## 9. Background Components

### 9.1 Boot Receiver

**File**: `receiver/BootReceiver.kt`  
**Intent**: `android.intent.action.BOOT_COMPLETED`

Checks `SettingsRepository.vpnAutoStart`. If enabled, starts `GateKeeperVpnService` with `ACTION_START`.

### 9.2 Device Admin Receiver

**File**: `receiver/GateKeeperDeviceAdminReceiver.kt`  
**Policy resource**: `res/xml/device_admin.xml`

Receives `DEVICE_ADMIN_ENABLED` / `DEVICE_ADMIN_DISABLED` broadcasts. Device admin capabilities are used for future hardware-level blocking features (e.g., disabling Wi-Fi or cellular radios programmatically).

### 9.3 Notification Manager

**File**: `notifications/GKNotificationManager.kt`

Centralised helper for posting notifications. Uses three channels:

| Channel ID | Importance | Purpose |
|---|---|---|
| `vpn_service` | LOW | Persistent foreground-service notification while VPN is running |
| `security_alerts` | HIGH | Alerts for blocked connections and security events |
| `gk_security_alerts` | HIGH + vibration | Critical-priority alerts (Evil Twin, IMSI catcher, exfiltration) |

Key methods:
- `postVpnNotification()` — persistent notification required by Android foreground service rules.
- `sendSecurityAlert(title, message, route)` — posts a high-priority alert with a deep-link `PendingIntent` to navigate to the relevant screen on tap.

---

## 10. Built-In DNS Blocklists

**File**: `data/model/BuiltInBlocklists.kt`

| ID | Name | Category | Estimated Size | Source |
|---|---|---|---|---|
| `stevenblack_unified` | Steven Black Unified | Ads / Malware / Social | ~130k domains | github.com/StevenBlack/hosts |
| `urlhaus_malware` | abuse.ch URLhaus | Malware | ~14k domains | urlhaus.abuse.ch |
| `adguard_dns` | AdGuard DNS Filter | Ads / Trackers | ~50k domains | adguardteam.github.io |
| `easyprivacy` | EasyPrivacy | Trackers | ~30k domains | v.firebog.net |
| `hagezi_pro` | HaGeZi Pro++ | Multi-purpose | ~200k domains | github.com/hagezi/dns-blocklists |

Users can add custom blocklist URLs from the DNS Filter screen. Custom lists are stored as `BlocklistSubscription` records in Room and can be refreshed on demand.

---

## 11. Utilities

### GeoIpResolver (`vpn/GeoIpResolver.kt`)

Uses the bundled **MaxMind GeoLite2-Country** database (`assets/GeoLite2-Country.mmdb`) for offline country lookups by IP address. Returns `CountryResponse` with ISO country code and name. Lookups are performed on the IO thread within `TrafficLogger`.

### GeoIpLookup (`util/GeoIpLookup.kt`)

A convenience wrapper around `GeoIpResolver` used by non-VPN components (e.g. UI screens).

### NetworkUtils (`vpn/NetworkUtils.kt`, `util/NetworkUtils.kt`)

Low-level IP byte manipulation helpers:
- Convert `ByteArray` ↔ `InetAddress`.
- Compute UDP/TCP checksums.
- Craft DNS response packets.
- Check if an IP is in a CIDR range.

### PcapWriter (`vpn/PcapWriter.kt`)

Writes captured packets to a PCAP file in standard libpcap format (magic number `0xa1b2c3d4`, link type `RAW`). Called from the VPN packet loop when PCAP recording is active (triggered by `ACTION_START_PCAP`).

### ExportUtils (`vpn/ExportUtils.kt`)

Exports `ConnectionLog` records to CSV (comma-separated) or JSON format for external analysis.

---

## 12. Permissions Reference

| Permission | Why It Is Needed |
|---|---|
| `INTERNET` | Backend API calls and DNS forwarding |
| `ACCESS_NETWORK_STATE` | Detect active network type (Wi-Fi vs cellular) |
| `ACCESS_WIFI_STATE` | Read current Wi-Fi SSID/BSSID for Evil Twin detection |
| `CHANGE_NETWORK_STATE` | Future: switch networks programmatically |
| `ACCESS_FINE_LOCATION` | Required by Android for Wi-Fi scan results (API 29+) |
| `ACCESS_COARSE_LOCATION` | Fallback location for Wi-Fi scanning |
| `FOREGROUND_SERVICE` | Run VPN as a foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for `android:foregroundServiceType="specialUse"` on API 34+ |
| `POST_NOTIFICATIONS` | Show VPN and security alert notifications (API 33+) |
| `QUERY_ALL_PACKAGES` | Enumerate all installed apps for the per-app firewall |
| `RECEIVE_BOOT_COMPLETED` | Auto-start VPN on device boot |
| `READ_PHONE_STATE` | Detect cellular network type for IMSI catcher detection |
| `BIND_VPN_SERVICE` | System permission binding the VPN service |
| `BIND_DEVICE_ADMIN` | Device administrator binding |

---

## 13. Build Configuration

**File**: `app/build.gradle.kts`

| Property | Value |
|---|---|
| `applicationId` | `com.gatekeeper.mobile` |
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` | 35 (Android 15) |
| `compileSdk` | 35 |
| `versionCode` | 1 |
| `versionName` | `1.0.0` |
| Kotlin version | 2.1.0 |
| AGP | 8.x (Ladybug+) |
| KSP | Used for Room and Hilt annotation processing |

#### Build Types

| Type | Minification | App ID Suffix |
|---|---|---|
| `debug` | Disabled | `.debug` |
| `release` | R8 + resource shrinking | (none) |

#### Key Dependencies

| Group | Library | Version |
|---|---|---|
| UI | Jetpack Compose BOM | `2024.12.01` |
| UI | Material 3 | `1.3.1` |
| UI | Vico Charts | `2.0.0-beta.2` |
| UI | Coil | `2.7.0` |
| DI | Hilt | `2.53.1` |
| Lifecycle | AndroidX Lifecycle/ViewModel | `2.8.7` |
| Navigation | Compose Navigation | `2.8.5` |
| Async | Coroutines | `1.9.0` |
| Storage | Room | `2.6.1` |
| Storage | DataStore | `1.1.1` |
| Network | Retrofit | `2.11.0` |
| Network | OkHttp | `4.12.0` |
| Security | MaxMind GeoIP2 | `4.2.0` |
| Background | WorkManager | Latest |
| Testing | JUnit 5, MockK, Turbine | — |

---

## 14. Database Migrations

| Migration | Changes |
|---|---|
| 2 → 3 | Add `known_networks` table |
| 3 → 4 | Add `sensor_logs` table |
| 4 → 5 | Add `security_alerts` table |
| 5 → 6 | Add `blocklist_subscriptions` table |
| 6 → 7 | Add `blockWhenScreenOff` column to `firewall_rules` |
| 7 → 8 | Add `isSystemEvent` and `systemEventReason` columns to `connection_logs` |

All migrations are registered in `AppModule.provideDatabase()` via `addMigrations()`.

---

## 15. Ecosystem Integration

GateKeeper Mobile is one component of the **GateKeeper Security Suite FYP**:

```
┌─────────────────────────┐     REST API      ┌──────────────────────────┐
│   GateKeeper Mobile     │ ◄───────────────► │   GateKeeper-Agent       │
│   (this repository)     │   POST /chat      │   (FastAPI AI Backend)   │
│   Android VPN + UI      │   GET /health     │   on Windows Desktop     │
└─────────────────────────┘                   └──────────────────────────┘
                                                          │
                                                          ▼
                                               ┌──────────────────────────┐
                                               │  GateKeeper Desktop App  │
                                               │  (React + Electron       │
                                               │   or Web Frontend)       │
                                               └──────────────────────────┘
```

- **On-device features** (firewall, DNS filter, traffic logging, threat detection) operate entirely offline.
- **AI features** require the GateKeeper-Agent backend to be reachable on the local network (configurable IP in Settings).
- The AI can execute firewall and DNS commands on the mobile app by responding with structured `tool_calls` in the chat response.

---

## 16. Development Setup

### Prerequisites

| Tool | Required Version |
|---|---|
| Android Studio | Ladybug (2024.2+) |
| JDK | 17 (strictly required) |
| Kotlin | 2.1.0 |
| Gradle | 8.13.2 (via wrapper) |
| Android Device | API 26+ (physical device recommended) |

### Build Steps

```bash
# 1. Clone the repository
git clone https://github.com/M-Fahim-Feroz/GateKeeper-Mobile.git
cd GateKeeper-Mobile

# 2. Open in Android Studio and allow Gradle sync

# 3. Enable USB Debugging on a physical Android device

# 4. Run the app
./gradlew installDebug
# OR use Android Studio → Run 'app'
```

> **Note**: The Android Emulator has known limitations with `VpnService` TUN packet routing. Physical devices are strongly recommended for testing the firewall, DNS filter, and traffic monitor.

### AI Backend Connection

1. Start the GateKeeper-Agent FastAPI server on port 8888.
2. In GateKeeper Mobile → Settings → AI Backend, enter the server's local IP address.
3. Tap "Test Connection" to verify connectivity.

---

## 17. Testing

The project is configured with the following test frameworks:

| Framework | Scope | Use |
|---|---|---|
| JUnit 5 | Unit tests | ViewModel logic, repository tests |
| MockK | Unit tests | Mocking dependencies in unit tests |
| Kotlin Coroutines Test | Unit tests | Testing `Flow` and `suspend` functions |
| Turbine | Unit tests | Testing `StateFlow`/`SharedFlow` emissions |
| Espresso | UI tests | Instrumented UI interaction tests |
| Compose UI Test | UI tests | Compose semantics-based UI testing |

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests (requires connected device):
```bash
./gradlew connectedAndroidTest
```

---

## 18. Security Features Quick-Reference

| Feature ID | Feature Name | Implementation | Alert Type |
|---|---|---|---|
| F1 | Per-App Firewall | `PacketFilter.blockedUids` | — |
| F2 | DNS Sinkhole | `DnsResolver` + `DnsBlocklistManager` | — |
| F3 | Traffic Monitor | `ConnectionTracker` + `TrafficLogger` | — |
| F4 | AI Security Assistant | `AiChatRepository` + `AiApiService` | — |
| F5 | Threat Intelligence | `ThreatFeedManager` | — |
| F6 | GeoIP Resolution | `GeoIpResolver` (MaxMind offline DB) | — |
| F7 | Blocklist Subscriptions | `DnsBlocklistManager.importFromUrl()` | — |
| F8 | Screen-Off Blocking | `PacketFilter.screenOffBlockedUids` | — |
| F9 | Evil Twin Detection | `RogueApDetector` | `EVIL_TWIN` (CRITICAL) |
| F10 | Open Network Warning | `RogueApDetector.OPEN_NETWORK` | Alert on connect |
| F11 | Certificate Auditor | `CertificateAuditor` | `ROGUE_CERT` (HIGH) |
| F12 | Permission Auditor | `PermissionScanner` | Risk score display |
| F13 | DNS Exfiltration Detection | `DnsBlocklistManager.checkDnsExfiltration()` | `DNS_EXFILTRATION` (HIGH) |
| F14 | Hardcoded-IP Bypass Detection | `DnsBlocklistManager.recentDnsResolutions` | Alert in VPN service |
| F15 | DoH Bypass Blocking | `PacketFilter.dohProviderIps` + `DNS_LEAK` verdict | Dropped silently |
| F16 | Data Exfiltration Detection | `ExfiltrationDetector` | `EXFILTRATION` (CRITICAL) |
| F17 | IMSI Catcher Detection | `CellularMonitor` | `IMSI_CATCHER` (CRITICAL) |
| F18 | PCAP Capture | `PcapWriter` | — |
| F19 | Log Export | `ExportUtils` | — |
| F20 | Privacy Access Logger | `PrivacyAccessLogger` | Sensor log entries |

---

<div align="center">
  <p><i>GateKeeper Mobile — Advanced Network Security Final Year Project</i></p>
</div>
