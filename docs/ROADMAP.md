# Kawach & Surokkha: Roadmap and status

Source of truth for the plan: the published blueprint, **v3 "parked features kept"**:
https://claude.ai/artifact/KmgD1JSuDM8cHUNu5JTrSr. A local copy is at [blueprint.html](blueprint.html).
Don't edit the blueprint's plan content. Record new decisions here instead.

Updated: 2026-10-31

## The two apps

- **Kawach**: a scam shield for elders, with a family guardian mode. **We build this first.**
- **Surokkha**: safe-commute trip logging for women on shared autos, e-rickshaws and city buses in Assam cities. It reuses Kawach's base (login, push notifications, backend) once Kawach is in beta.

## Launch rules: ₹0 for phase 1

- Free tiers only.
- Use the existing Google Play Console account.
- Web hosting on Vercel Hobby, at a `*.vercel.app` address.
- Test on your own phone and the `Pixel_7` emulator.
- Sign-in with Google, not SMS OTP. Elders join with an invite code.
- Kawach checks messages with rules at first. The AI (LLM) check and voice come later.
- Surokkha identifies vehicles by **number plate only**: no QR codes or stickers.
- **Parked, not dropped.** Each item comes back when its trigger happens (details in the blueprint's "Parked features" table):

  | Parked item | Bring it back when |
  |---|---|
  | LLM check | rules miss more than 20%, or revenue starts |
  | Voice | elders can't read the text cards |
  | QR pairing | elders mistype the invite code |
  | SMS OTP | users don't have a Google account |
  | Driver QR stickers | a police or union partnership is signed |
  | Paid SMS gateway | Play refuses the SMS permission, or business revenue starts |
  | Test phones | before the public launch |
  | Custom domain + Vercel Pro | before charging money |
  | Google Maps | the free map tiles run out |

- Free-tier catches:
  - Vercel Hobby can't be used commercially.
  - Supabase's free tier pauses after 7 days of inactivity.
  - A new Play account needs a closed test with 12 testers for 14 days before public release.
  - Play restricts SEND_SMS.
  - OpenStreetMap's public tile server isn't allowed for apps.

## Tech decisions made

- **Kawach is native Android: Kotlin + Jetpack Compose + Glance widgets.** The user wants full Android integration (widgets, call screening, notification access), which replaces Flutter from the blueprint.
- Build setup:
  - AGP 8.11.0, Kotlin 2.1.20, Gradle 8.14.3 (cached locally), compileSdk/targetSdk 36, minSdk 26.
  - JDK 21 is installed with scoop: `$env:JAVA_HOME="$HOME\scoop\apps\temurin21-jdk\current"`.
- App ID: `dev.wary.kawach`. It can't change after the first Play upload, so confirm it before uploading.
- Repo: https://github.com/007wary/kawach (private).

## Backend decision (confirmed 2026-10-31)

- **Kawach's backend is Firebase (Spark, free) + Vercel.** The user accepted my recommendation over Supabase.
  - Supabase's free tier already has 2 projects (creatorbull and Savr), so there's no free slot.
  - Supabase also pauses after 7 days, which is too risky for alerts.
  - Kawach needs FCM for push anyway.
  - A second Supabase account would break Supabase's fair-use terms.
  - How it would work: Firebase handles Google sign-in, Firestore and FCM. A Vercel function at `/api/notify` checks the Firebase ID token and sends the FCM push. Cloud Functions need the paid Blaze plan, so they're avoided.
- **Surokkha:** Supabase + PostGIS is likely the better fit later, because it needs map queries. Decide when Surokkha starts.

## Status

### Kawach milestone 1: DONE (commit `be19ad0`)

- Offline rule engine: 20 warning signs, 10 scam types, keywords in English, Hindi and Assamese. 14 unit tests pass.
- Screens: Home, Check, Result (with highlights), Help (1930 checklist), Learn.
- Share → Kawach intent, plus "Send to family" through the share sheet.
- Glance widget.
- English, Hindi and Assamese strings. The Hindi and Assamese are drafts that need native-speaker review.
- Checked on the emulator: home screen, and a shared digital-arrest message gives HIGH RISK with 5 warning signs and highlights.

## Cloud setup (done 2026-10-31)

### Firebase

- Project ID `kawach-wary` (display name "Kawach"), on the free Spark plan.
- **Created under 007cyberspawn@gmail.com.** The main account had hit its Google Cloud project quota, and Google's quota form needs a billing account.
- 007mwnswrangwary@gmail.com was invited as Owner. Accept the invitation email if you haven't yet.
- The CLI has both accounts. Use `--account 007cyberspawn@gmail.com` until the main account's Owner access is active.
- Android app `dev.wary.kawach`: App ID `1:817272712548:android:c3dbf4db3d8a422b2d9e71`.
  - Debug SHA-1 and SHA-256 are registered.
  - Before the Play release, add the release and Play App Signing SHA keys.
- `app/google-services.json` is downloaded and gitignored.
- Firestore `(default)`: Standard edition, asia-south1 (Mumbai), production-mode rules.
- Authentication: the Google sign-in provider is enabled (done in the console).

### Vercel

- Project `kawach` (scope 007warys-projects), deployed from `web/`.
- Production domain https://kawach-app.vercel.app. `kawach.vercel.app` was already taken.
- Pages: `/` (home) and `/privacy` (the privacy policy for the Play listing). The policy names the developer and shows the contact email 007mwnswrangwary@gmail.com.
- `/api/notify` comes in milestone 2.

## Next steps, in order

1. ~~Install and log in to the Firebase CLI~~: done.
2. ~~Confirm the backend~~: confirmed, Firebase + Vercel.
3. ~~Create the Firebase project and Android app~~: done (see Cloud setup above).
4. **Kawach milestone 2, family link:**
   - Google sign-in for the guardian.
   - Invite-code pairing for the elder.
   - Firestore schema: users, family_links, checks.
   - FCM push to the guardian through the Vercel `/api/notify` function.
   - Guardian alert screen with "Safe" and "Scam" replies.
5. ~~Vercel project and privacy policy~~: done. `/api/notify` is still to build, as part of milestone 2.
6. Milestone 3: screenshot check with on-device ML Kit OCR.
7. Milestone 4: weekly scam lessons, plus an admin panel on Vercel Hobby for editing patterns.
8. Play Console: internal testing, then a closed test (12 testers for 14 days; recruit through AssamJobDesk), then production.
9. Blueprint roadmap from here:
   - **Months 3–4:** beta with 100 families.
   - **Months 5–8:** family plan at ₹49/month, then turn on the parked LLM check and voice; add call screening and Bengali/Bodo.
   - **Months 9–12:** bank or cooperative pilot, iOS, grants.
10. **Surokkha** field validation runs during Kawach's beta:
    - survey 200 women commuters
    - map 20 routes
    - meet the Guwahati police women's cell
    - Then build milestone 1, reusing Kawach's base.
