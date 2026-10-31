# Kawach

[![CI](https://github.com/007wary/kawach/actions/workflows/ci.yml/badge.svg)](https://github.com/007wary/kawach/actions/workflows/ci.yml)

A scam shield for elders in India, with a family guardian mode. It's a native Android app (Kotlin + Jetpack Compose) with a small Vercel + Firebase backend.

- Blueprint: https://claude.ai/artifact/KmgD1JSuDM8cHUNu5JTrSr (local copy: [docs/blueprint.html](docs/blueprint.html))
- Plan, status and decisions: [docs/ROADMAP.md](docs/ROADMAP.md)
- Website and privacy policy: https://kawach-app.vercel.app

## What it does

- **Checks messages offline.** Rules cover 20 warning signs and 10 scam types, with keywords in English, Hindi and Assamese. The result shows highlighted red flags and the next steps (call 1930).
- **Gets text in many ways:** "Share → Kawach" from WhatsApp or Messages, paste, or the home-screen widget.
- **Family link (milestone 2).**
  - A guardian signs in with Google and gets a 6-digit code. The elder types the code on their phone.
  - Every check the elder makes goes to the guardians as a push alert, with phone and account numbers masked.
  - The guardian replies "safe" or "scam", and the reply comes back to the elder.
- **Languages:** English, हिन्दी, অসমীয়া, set per app on Android 13+.

## Repository layout

| Path | What's in it |
|---|---|
| `app/` | Android app. `rules/`: scam engine. `family/`: Firebase, API client, FCM, masking. `ui/`: Compose screens. `widget/`: Glance widget. |
| `web/` | Vercel project: the static site (`index.html`, `privacy.html`) and serverless functions in `web/api/`. |
| `web/api/` | `invite` (guardian code), `pair` (elder joins), `notify` (alerts and replies), `cleanup` (daily cron, deletes data older than 30 days). |
| `firebase/` | Firestore security rules and indexes, deployed with the Firebase CLI. |
| `docs/` | Roadmap and blueprint. |
| `.github/` | CI workflow, Dependabot, and the placeholder Firebase config that CI uses. |

## Set up a development machine

**You need:**
- JDK 21. On this machine it's installed with scoop.
- The Android SDK, with its path in `local.properties`.
- Node 22.
- The Firebase CLI and the Vercel CLI, both logged in.

1. **Firebase config.** `app/google-services.json` is gitignored. Download it with:
   ```powershell
   firebase apps:sdkconfig ANDROID 1:817272712548:android:c3dbf4db3d8a422b2d9e71 --project kawach-wary -o app/google-services.json
   ```
   Google sign-in only works if the build machine's debug SHA-1 is registered in the Firebase Android app.
2. **Build the app:**
   ```powershell
   $env:JAVA_HOME = "$HOME\scoop\apps\temurin21-jdk\current"
   .\gradlew.bat :app:testDebugUnitTest   # unit tests (rules, masking)
   .\gradlew.bat :app:lintDebug           # Android lint
   .\gradlew.bat :app:assembleDebug       # app\build\outputs\apk\debug\app-debug.apk
   .\gradlew.bat :app:installDebug        # install on a USB-connected phone
   ```
3. **Test the web API:**
   ```powershell
   cd web; npm ci; npm test; npm run check
   ```

## Deploying

- **Website and API:** push to `main`. Vercel builds from the `web/` folder (the project's Root Directory). Don't run `vercel deploy` from inside `web/`.
  - Production env vars, already set in Vercel and never committed:
    - `FIREBASE_SERVICE_ACCOUNT`: the service-account JSON.
    - `CRON_SECRET`: protects `/api/cleanup`.
- **Firestore rules and indexes:** `firebase deploy --only firestore` from the repo root. The project is `kawach-wary` (see `.firebaserc`).
- **Android:** Play Console release, not set up yet. See the roadmap.

## CI

GitHub Actions ([ci.yml](.github/workflows/ci.yml)) runs on every push to `main` and on every pull request:

- **Android:** unit tests, lint, and a debug build. It uses the placeholder [`.github/ci/google-services.json`](.github/ci/google-services.json), so the APK it builds can't reach Firebase.
- **Web:** `npm test` (masking tests that match `MaskTest.kt`) and a syntax check of every API function.

Dependabot opens grouped update PRs every month. Kotlin is pinned at 2.1.20, so check CI before merging library bumps.

## Before release

- Native speakers need to review the Hindi (`values-hi`) and Assamese (`values-as`) strings, and the native-script keywords in `Signals.kt`. They're drafts.
- `applicationId` is `dev.wary.kawach`. It can't change after the first Play upload.
- Add the release and Play App Signing SHA keys to the Firebase Android app.
