# eDompet: Android

Android port (Kotlin + Jetpack Compose) of the iOS `eDompet` app. It keeps membership
QR codes and barcodes in a wallet-style stack and syncs them to a Huawei watch (e.g. Watch GT 4)
through **Huawei Wear Engine**.

| iOS (Swift)                           | Android (Kotlin)                          |
|---------------------------------------|-------------------------------------------|
| `ContentView` / `TabView`             | `MainActivity` + `ui/GlassNavBar.kt`     |
| `AppleWalletView`, `WalletCardView`   | `ui/WalletScreen.kt`                      |
| `AddCardView`                         | `ui/AddCardScreen.kt`                     |
| `EditCardSheet`                       | `ui/EditCardSheet.kt` (bottom sheet)      |
| `ScannerViewController` (AVFoundation)| ZXing `ScanContract` (no Google Play needed) |
| `CIDetector` on a photo               | `scan/CodeUtils.kt` `decodeFromImage` (QR **and** barcodes) |
| `UserDefaults`                        | `data/CardRepository.kt` (SharedPreferences, same key) |
| `WearEngineManager.swift`             | `wear/WearEngineManager.kt`               |

The JSON sent to the watch keeps the iOS keys (`merchantName`, `codeType`, `codeValue`,
`backgroundColor`), so an existing watch app keeps working. It also adds `id`, `action`
(`"upsert"`, or `"delete"` with just an `id`), `gradientColors` (`[top, bottom]` hex) and
`textColor`. Sync is automatic: changes are queued and sent one at a time whenever the watch is
connected, and pending work survives app restarts.

Card backgrounds are 12 two-stop gradients taken from the "color gradient" palettes. They are
ordered by the rainbow (red, orange, yellow, green, blue, indigo, violet), then pink, maroon and
black. Each preset has a fixed text color, checked against WCAG contrast. See `CardStyle` in
`data/MembershipCard.kt`.

New in this version: a **Show code** button on an expanded card shows a large QR code or barcode
for the cashier. **Add & Send to Watch** now also sends the card to the watch (on iOS it only
saved it).

Navigation is a floating glass bar (`ui/GlassNavBar.kt`): a rounded pill with the tabs (the
selected tab shows its label) and a round search button. Search filters the wallet by merchant
name **or** code number. Name matches rank first, and codes match even when you leave out the
spaces or dashes printed on the card. See `data/CardSearch.kt`.

## Build output
Build output goes to `build.nosync/` instead of `build/`. `~/Documents` is synced by iCloud Drive,
which creates "BuildConfig 2.java"-style conflict copies inside `build/`, and those break the build.
iCloud skips folders whose names end in `.nosync`.

## Open & run
1. Open the `android/` folder in **Android Studio** (Ladybug or newer). It downloads Gradle 8.11.1,
   the Android SDK and the dependencies, including Wear Engine from `developer.huawei.com/repo`.
2. Run it on a real phone. The phone needs the **Huawei Health** app, with the watch paired in Health.
   This works on any Android phone, not only Huawei phones.

## Huawei setup (required for watch sync)
1. In the Huawei Developer console, apply for the **Wear Engine** service for the Android app
   `com.ipandai.edompet`. Register the **SHA-256 fingerprint** of the signing certificate you build with:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
   Register the debug keystore for testing and the release keystore for production.
2. In `wear/WearEngineManager.kt`, set:
   - `WATCH_PKG_NAME`: the bundle name of the watch app (`com.ipandai.edompet.watch`; AGC does not allow a HarmonyOS app to reuse the Android package name).
   - `WATCH_FINGERPRINT`: the watch app's signing fingerprint, from DevEco Studio / AppGallery Connect.
3. The **watch app** (built in DevEco Studio) must be installed on the GT 4 and must listen for P2P
   messages from the phone package `com.ipandai.edompet`.

`agconnect-services.plist` from iOS is not used here. Wear Engine on Android identifies the app by
its package name and signing certificate.
