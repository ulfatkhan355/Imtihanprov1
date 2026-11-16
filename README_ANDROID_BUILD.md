# Imtihan Pro — Android Build & Physical Device Test Guide (Ubuntu)

This guide explains how to open, prepare, test, and build the native Android student application (`android/`) on **Ubuntu** using either **Android Studio** or **command-line Gradle**, and how to install the debug APK on a physical Android phone.

> **Honest Build Notice**: This container environment has Node.js tooling only (no JDK 17, Gradle, or Android SDK). The Android sources have been statically audited and prepared for **JDK 17**, **Android SDK 35**, **AGP 8.7.2**, **Kotlin 2.0.21**, and **Gradle 8.9**, and the Gradle wrapper JAR (`gradle-wrapper.jar`) is intentionally **not** fabricated here. Generate or sync the wrapper on Ubuntu using the commands below.

---

## 1. Prerequisites on Ubuntu

- **JDK 17** (required by AGP 8.7.2 & `compileOptions` / `kotlinOptions`)
- **Android SDK Platform 35** (`platforms;android-35`) & Build Tools (`35.0.0` / `34.0.0`)
- **Gradle 8.9** (for first-time wrapper initialization) or **Android Studio Ladybug (2024.2+)**
- **ADB (`android-tools-adb` / SDK Platform-Tools)** for physical device testing

### Verify Java Version (Must be JDK 17)
```bash
java -version
javac -version
```
If multiple JDKs are installed on Ubuntu, switch or export `JAVA_HOME`:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

### Verify Android SDK Setup
```bash
echo "$ANDROID_HOME"
ls "$ANDROID_HOME/platforms"
ls "$ANDROID_HOME/platform-tools/adb"
```
If `android-35` is not installed yet:
```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```
Create `android/local.properties` (if not using Android Studio):
```bash
echo "sdk.dir=$ANDROID_HOME" > android/local.properties
```

---

## 2. Option A: Open & Build in Android Studio (Ubuntu)

1. Launch **Android Studio** (Hedgehog / Koala / Ladybug or newer).
2. Click **Open** and select the **`android/`** folder inside this repository (not the repository root).
3. Ensure **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** is set to **JDK 17**.
4. Wait for **Gradle Sync** to download dependencies and generate/configure the Gradle wrapper.
5. Connect a physical Android phone (Android 7.0+ / API 24–35) with **USB Debugging** enabled.
6. Select your phone in the device dropdown and click **Run (`app`)**.

---

## 3. Option B: Command-Line Gradle Build on Ubuntu

Move into the Android project directory first:
```bash
cd android
```

### Step 1: Generate and Verify the Gradle Wrapper (One-Time Setup)
Because `gradle-wrapper.jar` is not committed or fabricated in this repo, run:
```bash
gradle wrapper --gradle-version 8.9
chmod +x gradlew
./gradlew --version
```

### Step 2: Run JVM Unit Tests
```bash
./gradlew testDebugUnitTest
```
Unit test reports will be written to:
`android/app/build/reports/tests/testDebugUnitTest/index.html`

### Step 3: Build the Debug APK
```bash
./gradlew assembleDebug
```
Locate the generated debug APK at:
```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Build the Release APK (Minified + Shrunk)
`app/build.gradle.kts` enables R8 code shrinking (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) using `app/proguard-rules.pro`.
```bash
./gradlew assembleRelease
```
Locate the unsigned release APK at:
```text
android/app/build/outputs/apk/release/app-release-unsigned.apk
```
*(Configure a `signingConfig` keystore in `app/build.gradle.kts` or sign with `apksigner` before distribution.)*

---

## 4. Install & Test on a Physical Android Phone (ADB)

1. On your Android phone: **Settings → About phone → tap Build number 7 times** to unlock Developer options.
2. Enable **Settings → Developer options → USB debugging**.
3. Connect the phone via USB and authorize the RSA prompt on the phone screen.
4. Verify ADB detects the device:
   ```bash
   adb devices
   ```
5. Install the Debug APK:
   ```bash
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```
6. Launch the app directly from terminal (optional):
   ```bash
   adb shell am start -n com.imtihanpro.app/.MainActivity
   ```

---

## 5. Physical Device Verification Checklist (Stage 1 Offline)

- **Cold Launch & Splash**: Launches into dark Silver Blue splash screen (`#060E21`) and routes cleanly to First-Time Setup (first run) or Home Dashboard (subsequent runs, persisted via DataStore).
- **First-Time Setup**: Select Class (9/10/11/12), Board (`KPK` active), optional name; keyboard does not obscure buttons; **Continue as Guest** lands on Home.
- **Navigation & Back Stack**: Test 5 bottom tabs (`Home`, `Subjects`, `Practice`, `Papers`, `Profile`) and hardware/gesture Back navigation.
- **Theme Switching**: Toggle **Theme** in `Profile` between Dark Mode and Light Mode and confirm status/navigation bar icon contrast updates cleanly.
- **Learning Flow**: `Home` → `Subjects` → `Physics` → `Chapter 1` → `Detailed Notes` / `Revision Notes` / `MCQ Practice` (verify scrollability + timer + `Quiz Results`) / `Solved Questions`.
- **Past Papers & PDF Reader**: Filter `All` / `Solved` / `Unsolved`, open PDF viewer, adjust zoom and pages, toggle offline download, check `Downloads`.
