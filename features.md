# GateKeeper Mobile Features

This document provides a comprehensive list of all the functionalities available in GateKeeper Mobile, organized into logical categories for presentation slides. It uses the exact names and descriptions as they appear in the application's user interface.

## 1. Network Protection
*These features control and secure your device's basic network traffic.*

- **App Gate**
  Controls which apps can use the internet. You can set granular rules on a per-app basis to allow or block Wi-Fi and Mobile Data natively at the device level without needing root access.
- **Web Gate**
  Blocks dangerous websites and ads. It works like a bouncer at the door — dangerous sites, ads, and trackers are blocked at the network level before they ever reach your browser or apps. Includes **Safe Search** to automatically enforce safe browsing on major search engines.
- **Block apps when screen is off**
  Selected apps can't access the internet while your screen is off. This acts as a background data transmission stop when the device is locked.
- **DNS privacy guard**
  Stops apps from sneaking past the DNS filter using encrypted DNS (DNS-over-HTTPS leak prevention).
- **Bypass attempt detector**
  Alerts you if a blocked app tries to connect using a hardcoded server address to bypass the VPN.

## 2. Privacy & Data Security
*These features monitor how apps use your device sensors and prevent silent data harvesting.*

- **Privacy Logs**
  Shows when apps access your mic, camera or sensors. It analyzes all installed applications and assigns risk scores based on their declared sensitive permissions.
- **Secret data leak detector**
  Alerts if an app tunnels data through DNS queries. It uses Shannon entropy to detect data exfiltration attempts.
- **Background camera & mic alerts**
  Notifies you when apps access your camera or mic while running in the background.
- **Global camera block**
  Completely disables the camera system-wide for all apps (requires Device Admin permission).

## 3. Threat Detection
*These features proactively scan your environment for external attacks.*

- **Threat Intel**
  Blocks known malware and hacker servers. It automatically cross-references all active network connections with public threat intelligence feeds to instantly drop malicious packets.
- **Wi-Fi Guard & Fake Wi-Fi detector**
  **Wi-Fi Guard** detects fake and dangerous wireless networks by evaluating their security configuration. The **Fake Wi-Fi detector** actively detects duplicate Wi-Fi networks (Evil Twins) that may be trying to steal your connection by tracking and verifying router BSSIDs.
- **Fake cell tower detector**
  Warns you if a fake cell tower is trying to intercept your calls by monitoring for suspicious 2G network downgrades (IMSI Catcher detection).
- **Trust Check**
  Scans for suspicious security certificates. It scans the user-installed CA certificates on your device and identifies known Man-in-the-Middle (MITM) proxies, expired certificates, or rogue authorities.

## 4. Monitoring & Forensics
*These features provide deep visibility into network activity for analysis.*

- **NetWatch**
  Visualize your network activity with live, real-time bandwidth tracking. It maintains detailed connection logs for every application and provides MaxMind GeoIP country resolution for all destinations.
- **PCAP Traffic Capture**
  Record raw packets to .pcap file for Wireshark analysis. Captures raw network traffic directly from the device (max 50 MB per file, auto-deleted after 24 hours).
- **Export Traffic Logs**
  Save connection logs as CSV for external forensic analysis.
- **Export Firewall Rules**
  Save current firewall rules as JSON.
