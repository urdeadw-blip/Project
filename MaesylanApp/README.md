# Maesylan Android App

A native Android WebView wrapper for the **Maesylan Boarding Kennels** website, built in Kotlin.

## Features

- 🚀 **Splash screen** — branded splash on launch (Android 12+ SplashScreen API + fallback)
- 🔄 **Pull-to-refresh** — swipe down to reload the page
- 📴 **Offline handling** — friendly error screen with retry button when no internet
- 🔗 **Smart link handling** — internal links stay in-app; external domains open in device browser
- ⬅️ **Back navigation** — back button walks WebView history before exiting
- 📱 **Responsive** — works on phones and tablets (adaptive layouts)
- 🎨 **Brand-matched** — uses the same cyan paw-print logo and colour palette as the website

## App Details

| Property | Value |
|----------|-------|
| **App Name** | Maesylan |
| **Package** | `com.maesylan.app` |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 35 (Android 15) |
| **Language** | Kotlin |
| **Website** | `https://urdeadw-blip.github.io/Project/` |

## Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.2) or newer
- **JDK 17** or newer
- Android SDK with API level 35

### Building

1. Open the `MaesylanApp/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Click **Run** ▶ to build and install on a device/emulator

### Building from Command Line

```bash
cd MaesylanApp
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK (requires signing config)
```

The debug APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Signing for Release

To build a signed release APK, add to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("your-keystore.jks")
            storePassword = "..."
            keyAlias = "..."
            keyPassword = "..."
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Or use Android Studio's **Build → Generate Signed Bundle / APK** wizard.

## Configuration

The website URL is defined in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "WEBSITE_URL", "\"https://urdeadw-blip.github.io/Project/\"")
buildConfigField("String", "PRIMARY_HOST", "\"urdeadw-blip.github.io\"")
```

To point the app at a different URL (e.g., a custom domain), update these values and rebuild.

### Internal vs External Links

Links are considered **internal** (stay in-app) if they match:
- The `PRIMARY_HOST` (e.g., `urdeadw-blip.github.io`)
- Any subdomain of `maesylankennels.co.uk`
- Anchor links (`#section`) and `about:blank`

All other links open in the device's default browser. Special schemes (`tel:`, `mailto:`, `whatsapp:`, `sms:`) are always passed to the OS.

## GitHub Pages Setup

The app loads the website from GitHub Pages. Make sure Pages is enabled:

1. Go to **Settings → Pages** on the `urdeadw-blip/Project` repository
2. Set **Source** to "Deploy from a branch"
3. Select **Branch**: `main` and folder `/ (root)`
4. Click **Save**

The site will be available at `https://urdeadw-blip.github.io/Project/`

## Project Structure

```
MaesylanApp/
├── app/
│   ├── build.gradle.kts          # App-level build config
│   ├── proguard-rules.pro        # ProGuard/R8 rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/maesylan/app/
│       │   ├── MainActivity.kt   # WebView host with all features
│       │   └── SplashActivity.kt # Launcher splash screen
│       └── res/
│           ├── drawable/         # Vector icons (paw logo, error, retry)
│           ├── layout/           # Activity layouts
│           ├── mipmap-*/         # App icons (all densities)
│           ├── values/           # Colors, strings, themes
│           ├── values-sw600dp/   # Tablet-specific dimensions
│           ├── values-v31/       # Android 12+ splash config
│           └── xml/              # Network security config
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   └── wrapper/
├── build.gradle.kts              # Project-level build config
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

## Brand Colours

Extracted from the website's CSS tokens:

| Name | Hex | Usage |
|------|-----|-------|
| Cyan | `#2AAAC6` | Primary actions, logo |
| Cyan Dark | `#218AA2` | Hover states |
| Red 950 | `#1B0A0A` | Splash background |
| Red 900 | `#240E0E` | Icon background |
| Cream | `#FAF6EE` | App background |
| Mist | `#7F9AA8` | Muted text |

## License

This project is proprietary to Maesylan Boarding Kennels.
