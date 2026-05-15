<p align="center">
  <img src="./logo/KRISHI_PRABANDH_LOGO.png" width="120" alt="Krishi Prabandh Logo" />
</p>

<h1 align="center">Krishi Prabandh: SwaSurvey App</h1>

<p align="center">
  <strong>Offline-First Agricultural Survey Application</strong>
</p>

<p align="center">
  <a href="./DETAIL.md">Technical Documentation</a> &nbsp;&bull;&nbsp;
  <a href="../README.md">Main Project</a>
</p>

---

**SwaSurvey** (E-Peek Pahani) represents a monumental leap forward—a digital transformation of agricultural land workflows. It decentralizes data entry by putting a robust digital tool directly into the hands of farmers, enabling them to self-report their crop status directly to the government without bureaucratic delays.

Built specifically for the Android platform, it offers an intuitive, localized interface that understands the reality of rural infrastructure, prioritizing offline capabilities and seamless edge synchronization.

---

## Core Features

### 🧑‍🌾 Decentralized Self-Reporting
Farmers leverage their smartphones to capture geotagged, timestamped images of their crops, instantly tying their agricultural status to their official profiles.

### 📶 Robust Offline Synchronization
A powerful RoomDB local database architecture ensures that farmers can capture data deep in the fields without internet connectivity. Data is safely cached and seamlessly synced with the cloud once network access is restored.

### 📍 Precision Geofencing
Validates crop photos mathematically by cross-referencing embedded GPS metadata against government-registered land boundaries, preventing fraudulent claims.

### 🌍 Multilingual UI
Thoughtfully localized interface supporting regional languages to ensure maximum accessibility for diverse farming communities.

---

## Quick Start (Android Studio)

### Prerequisites
- **Android Studio:** Ladybug or latest stable version.
- **Java Development Kit:** JDK 21.
- **Android SDK:** API Level 34 (Minimum API 24).

### Setup

1. **Clone & Open:** Open the `Krishi Prabandh SwaSurvey App` directory in Android Studio.
2. **Local Properties:** Ensure `local.properties` exists with your SDK path:
   ```properties
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY="your_google_maps_api_key_here"
   ```
3. **Build:** Sync the project with Gradle files.
4. **Run:** Deploy the `assembleDebug` build to an emulator or physical device.

---

For an in-depth breakdown of the MVVM Architecture and Tech Stack, see [DETAIL.md](./DETAIL.md).
