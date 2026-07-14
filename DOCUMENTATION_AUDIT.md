# Documentation Audit

| Claim | Actual Implementation | Source File | Status | Required Change |
|---|---|---|---|---|
| Kotlin 2.1 | Kotlin 2.2.10 | libs.versions.toml | Outdated | Update to 2.2.10 |
| Android Gradle Plugin not specified | AGP 9.2.0 | libs.versions.toml | Missing | Add AGP 9.2.0 |
| Gradle version not specified | Gradle 9.4.1 | gradle-wrapper.properties | Missing | Add Gradle 9.4.1 |
| DB version 8 | DB version 12 | AppDatabase.kt | Outdated | Update to version 12, list migrations up to 8_9 |
| MTU 32767 | MTU 16384 | GateKeeperVpnService.kt | Outdated | Update MTU to 16384 |
| No Bidirectional TCP/UDP forwarding | TCP/UDP RelayHandlers exist | GateKeeperVpnService.kt, vpn/relay/ | Missing from documentation | Document full TCP/UDP relay forwarding (Bidirectional) |
| Alerts embedded in Dashboard | AlertsScreen exists | ui/screens/alerts/AlertsScreen.kt | Outdated | Document new Alerts route/screen |
| ProtectHub missing | ProtectHubScreen exists | ui/screens/protecthub/ProtectHubScreen.kt | Missing from documentation | Document ProtectHub screen |
| Testing commands | No tests present in codebase | app/src/test | Documented but not implemented | State that there are no automated tests currently implemented |
| DataStore Keys | SettingsRepository has 14 keys (e.g. imsi_detection, safe_search_enabled) | SettingsRepository.kt | Outdated | Update DataStore keys list |

