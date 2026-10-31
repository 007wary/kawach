# Kawach

Native Android scam shield for elders, with a family guardian mode (Kotlin + Jetpack Compose).
Blueprint: https://claude.ai/artifact/KmgD1JSuDM8cHUNu5JTrSr

## What works now (milestone 1, fully offline, ₹0)

- Rule-based scam checker: 20 warning signs, 10 scam types, English, Hindi and Assamese keywords (`app/src/main/java/dev/wary/kawach/rules`)
- Screens: Home, Check, Result (highlighted red flags), Help (1930 checklist), Learn
- "Share → Kawach" from WhatsApp, Messages or any app that shares text
- "Send to family" through the share sheet (free stand-in until guardian push alerts)
- Home-screen widget: "Check a message" + "Help · 1930"
- App language: English, हिन्दी, অসমীয়া (Android 13+ per-app language setting)

## Build

Requires JDK 17+ (JDK 21 is installed through scoop) and the Android SDK (path in `local.properties`).

```powershell
$env:JAVA_HOME = "$HOME\scoop\apps\temurin21-jdk\current"
.\gradlew.bat :app:testDebugUnitTest    # rule-engine tests
.\gradlew.bat :app:assembleDebug        # app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat :app:installDebug         # install on a phone connected with USB debugging
```

## Before release

- Hindi (`values-hi`) and Assamese (`values-as`) text, and the native-script keywords in `Signals.kt`, are drafts: have native speakers review them.
- `applicationId` is `dev.wary.kawach`. It can't change after the first Play upload.

## Next milestones

2. Guardian link: Google sign-in, invite code, Supabase (free tier), FCM push alerts
3. Screenshot check with on-device ML Kit OCR
4. Weekly scam lessons + admin panel (Vercel Hobby)

Parked for later (see the blueprint's "Parked features"): LLM check, voice explanations, QR pairing, SMS OTP, call screening.
