<div align="center">
  <h1>🛡️ GateKeeper Mobile</h1>
  <p><b>AI-powered mobile security suite for Android</b></p>
  <p><i>Combining per-app firewall, DNS filtering, traffic monitoring, and intelligent threat detection into one powerful application.</i></p>
</div>

---

## 🌟 Overview

GateKeeper Mobile is the mobile counterpart of the GateKeeper Security Suite. Designed with a modern **Jetpack Compose** interface, it utilizes Android's `VpnService` to provide system-wide network protection without requiring root access.

## ✨ Key Features

* **🛡️ Per-App Firewall**: Granular control over which applications can access Wi-Fi or Mobile Data. Intercepts and blocks unauthorized connections natively.
* **🌐 DNS Sinkhole & Filtering**: Block intrusive ads, tracking domains, and known malware at the DNS level before the requests ever leave the device.
* **📡 Real-Time Traffic Monitor**: Visualize your network activity with live bandwidth tracking, GeoIP resolution, and connection logs per application.
* **🤖 AI Security Assistant**: Secure your device using natural language. The integrated AI (powered by the GateKeeper-Agent backend) allows you to say commands like *"Block Facebook"* or *"Show me recent threats"*.
* **🔍 Threat Intelligence Integration**: Automatically cross-reference active connections with public threat feeds to instantly drop malicious packets.

---

## 📸 Screenshots

*(Add screenshots of the Dashboard, Firewall settings, Traffic Charts, and AI Chat here once available)*

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

While the firewall, DNS, and traffic logging work entirely on-device, the app integrates with the **GateKeeper-Agent** for advanced AI interactions:
* **GateKeeper-Agent (AI Backend)**: Processes natural language commands sent from the mobile app via the `/chat` REST API.
* **Desktop Application**: The rules and activity logs can optionally be synchronized and viewed on the desktop React frontend.

---

<div align="center">
  <p>Built as part of an Advanced Network Security Final Year Project.</p>
</div>
