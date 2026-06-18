<div align="center">
  <h1>🛡️ GateKeeper Mobile</h1>
  <p><b>Mobile security suite for Android</b></p>
  <p><i>Combining per-app firewall, DNS filtering, traffic monitoring, and intelligent threat detection into one powerful application.</i></p>
</div>

---

## 🌟 Overview

GateKeeper Mobile is the mobile counterpart of the GateKeeper Security Suite. Designed with a modern **Jetpack Compose** interface, it utilizes Android's `VpnService` to provide system-wide network protection without requiring root access.

## ✨ Key Features

* **🛡️ Per-App Firewall**: Granular control over which applications can access Wi-Fi or Mobile Data. Intercepts and blocks unauthorized connections natively, including a global screen-off blocking mode.
* **🌐 DNS Sinkhole & Filtering**: Block intrusive ads, tracking domains, and known malware at the DNS level before requests leave the device. Supports built-in lists, custom subscriptions, and DNS exfiltration detection.
* **📡 Real-Time Traffic Monitor**: Visualize your network activity with live bandwidth tracking, MaxMind GeoIP resolution, and exportable connection logs per application.
* **🔍 Threat Intelligence Integration**: Automatically cross-reference active connections with public threat feeds to instantly drop malicious packets.
* **📶 Evil Twin Detection**: Scans Wi-Fi networks and alerts you when connecting to a suspicious Access Point with an unknown BSSID.
* **📱 IMSI Catcher Detection**: Monitors cellular connection states and alerts you on suspicious 2G network downgrades indicating a potential Stingray attack.
* **🕵️ Data Exfiltration Detection**: Correlates recent microphone and camera access with large outbound network uploads to detect privacy leaks in real-time.
* **🔐 Certificate Auditor**: Scans user-installed CA certificates and identifies known MITM proxies or expired certificates.
* **📊 Permission Auditor**: Analyzes all installed apps and assigns risk scores based on their declared sensitive permissions.
* **🛑 Leak Prevention**: Detects and drops attempts by apps to bypass the VPN using custom DNS-over-HTTPS (DoH) resolvers or hardcoded IP addresses.
* **💾 PCAP Capture**: Record raw network traffic to standard PCAP files directly from the device for external forensic analysis.

---

## 📸 Screenshots

*(Add screenshots of the Dashboard, Firewall settings, and Traffic Charts here once available)*

---

## 🛠️ Tech Stack & Architecture

GateKeeper Mobile is built using modern Android development best practices (Clean Architecture + MVVM).

| Category | Technologies Used |
|---|---|
| **Language** | Kotlin 2.1 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Dependency Injection** | Hilt (Dagger) |
| **Local Storage** | Room (SQLite), DataStore |
| **Networking/API** | Retrofit, OkHttp |
| **Charting** | Vico Charts |
| **Security Engine** | Android `VpnService`, TUN Interface, Custom Packet Filter |
| **Asynchrony** | Coroutines & Flow |

### Project Structure (Key Modules)

```text
GateKeeper-Mobile/
├── app/src/main/java/com/gatekeeper/mobile/
│   ├── vpn/            # Core VPN engine (Packet routing, IPv4/IPv6, DNS intercept)
│   ├── data/           # Repositories, Room DAOs, Retrofit APIs
│   ├── ui/             # Jetpack Compose screens, ViewModels, Theme
│   └── di/             # Hilt Dependency Injection modules
```

---

## ⚙️ Getting Started

For detailed tool versions and environment setups, please refer to [REQUIREMENTS.md](REQUIREMENTS.md).

### Prerequisites
* **Android Studio Ladybug (2024.2+)**
* **JDK 17** 
* Physical Android Device running **Android 8.0+** (API 26+)

### Installation & Build

1. Clone the repository and open the `GateKeeper-Mobile` folder in Android Studio.
   ```bash
   git clone https://github.com/M-Fahim-Feroz/FYP.git
   cd FYP/GateKeeper-Mobile
   ```
2. Allow Gradle to sync the project dependencies.
3. Enable **USB Debugging** on your physical Android device and connect it to your machine.
   *(Note: Android emulators often fail to route packets through custom VPN interfaces efficiently. Physical devices are highly recommended for testing).*
4. Click **Run 'app'** in Android Studio.

---

## 🔗 Ecosystem Integration

GateKeeper Mobile is one piece of the larger **GateKeeper Security Suite FYP**. 

The firewall, DNS, and traffic logging work entirely on-device, providing robust and independent protection.
* **Desktop Application**: The rules and activity logs can optionally be synchronized and viewed on the desktop React frontend.

---

<div align="center">
  <p>Built as part of an Advanced Network Security Final Year Project.</p>
</div>
