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
  - Firestore TTL policies need billing, so Spark can't use them. Old data is deleted by a daily Vercel Cron instead.
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
- Repo: https://github.com/007wary/kawach (public for now, no license). Never commit secrets. Push straight to `main`: no PRs, no Dependabot.

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

### Kawach milestone 2, family link: DONE

- **Roles.** The guardian signs in with Google. The elder gets an anonymous Firebase account and types the guardian's 6-digit code. One phone is either an elder or a guardian.
- **Pairing.** `POST /api/invite` gives the guardian a code that works once, for 15 minutes. `POST /api/pair` links the elder (up to 3 guardians). Each account gets 5 wrong codes an hour.
- **Alerts.** When a linked elder checks a message, every check goes to the guardians (blueprint: "the guardian always sees the message"). `POST /api/notify`:
  - Masks phone and account numbers, on the phone and again on the server.
  - Saves the check and sends a data-only FCM push.
  - The guardian replies "safe" or "scam" on the alert screen, and the elder gets a push and sees the reply on the result screen.
- **Call button.** The guardian saves the elder's phone number on their own phone only. It's never uploaded.
- **Firestore.** Clients can only read; all writes go through the Vercel API (Admin SDK). The exceptions are each user's own `users/{uid}` doc and unlinking. Rules and indexes are in `firebase/`.
  - `users/{uid}`: role, language, fcm_tokens, updated_at
  - `family_links/{elderId}_{guardianId}`: elder_id, guardian_id, elder_name, guardian_name, invite_code, status, created_at
  - `checks/{elderId}_{clientId}`: elder_id, elder_name, guardian_ids[], input_type (text | image | other), masked_text, verdict, scam_type, red_flags[], guardian_reply, created_at, expire_at
  - `invites/{code}` and `pair_attempts/{uid}`: server only
- **30-day deletion.** Firestore TTL needs billing, so `/api/cleanup` runs as a daily Vercel Cron at 02:00 IST.
- 4 new unit tests for the masking.

**Cloud setup: done 2026-10-31.**

- Anonymous sign-in is enabled.
- Firestore rules and the index are deployed.
- `FIREBASE_SERVICE_ACCOUNT` and `CRON_SECRET` are in Vercel production, and `web/` is deployed.

**Tested end to end on 2026-10-31.** The `Pixel_7` emulator was the guardian (007cyberspawn), and a script called the API as the elder.

- Google sign-in, and a code from `/api/invite`.
- A wrong code is rejected, the right code links, and a reused code is rejected.
- The elder's check reaches the emulator as a push ("Test Maa checked a message · HIGH RISK · Digital arrest scam").
- The alert screen shows the warning signs and highlights, with the numbers masked.
- The "scam" reply is saved, and the elder reads it back through the rules.
- The rules deny an elder reading another user's doc, writing checks directly, or replying through the API.
- Found and fixed: the FCM token was saved only at sign-in, so one network failure meant no alerts ever. It's now saved again on every app start (`Family.syncToken`).
- **Elder side, tested on the user's real phone (2026-10-31):** code entry → linked to Cyber Spawn → a KYC message checked → "Sent to…" card → the guardian replied "scam" from the emulator → the reply came back to the phone.

### Kawach milestone 3, screenshot check: DONE

- **Two ways in:**
  - "Share → Check with Kawach" now accepts images too (from Gallery, Files, WhatsApp). A caption shared with the picture is checked with it.
  - "Check a screenshot" button on the Check screen. It uses Android's photo picker, so it needs no storage permission. Some phones say they have the picker but can't open it (the `Pixel_7` emulator crashed this way), so it falls back to the standard file chooser.
- **OCR on the phone:** ML Kit text recognition, Devanagari model, through Google Play services (`play-services-mlkit-text-recognition-devanagari` 16.0.2), so the APK stays small. The model is listed in the manifest (`ocr_devanagari`), so Play installs it with the app. On a sideloaded app it downloads on first use: Kawach asks Play services for it and waits up to 40 seconds before showing "still getting ready".
- **Privacy:** the picture never leaves the phone. Only the words read from it go to family, masked like any check. `checks.input_type` is now `text`, `image` or `other`; there's no new field. The privacy policy now covers screenshots.
- The result screen says "Kawach read these words from the picture… if words are missing or wrong, paste the message instead".
- **Tested on the emulator (2026-10-31)** with drawn chat screenshots:
  - English + Hindi KYC scam → HIGH RISK, fake bank/KYC, through both the share sheet (Files app) and the Check screen button.
  - Assamese KYC scam: **ML Kit does read Assamese (Bengali) script**, with a few letters wrong ("কৰক" → "কবক", a stray hasanta in "কওক"). That first gave a wrong LOW result. Fixed by adding "KYC আপডেট", "KYC अपडेट" and the "একাউন্ট" spelling to the account-block rule. Now HIGH, with a unit test on the exact OCR text.
  - Error screen when a picture can't be opened.
- **Seen once:** right after first use, Play services killed Kawach to update its shared TFLite module ("Module config changed, forcing restart"). That's Play services behaviour, not a Kawach bug, and it didn't happen again.
- **Not tested yet:** a real WhatsApp image share, and the founder's phone.

**Follow-ups:**

- Rules miss more when OCR misreads Assamese letters. Collect real screenshots in beta and add the common misreadings, or count it towards the "rules miss more than 20%" trigger for the LLM check.
- Panic mode: "I already paid" on the Help screen alerts the guardians.
- When a guardian unlinks, they can still read that elder's old checks until they're deleted at 30 days.

## Cloud setup (done 2026-10-31)

### Firebase

- Project ID `kawach-wary` (display name "Kawach"), on the free Spark plan.
- **Created under 007cyberspawn@gmail.com.** The main account had hit its Google Cloud project quota, and Google's quota form needs a billing account.
- 007mwnswrangwary@gmail.com is an **Owner** (accepted and confirmed). The main account manages it like any other project, and CLI commands work without `--account`.
- Android app `dev.wary.kawach`: App ID `1:817272712548:android:c3dbf4db3d8a422b2d9e71`.
  - Debug SHA-1 and SHA-256 are registered.
  - Before the Play release, add the release and Play App Signing SHA keys.
- `app/google-services.json` is downloaded and gitignored.
- Firestore `(default)`: Standard edition, asia-south1 (Mumbai), production-mode rules.
- Authentication: the Google sign-in provider is enabled (done in the console).

### Vercel

- Project `kawach` (scope 007warys-projects), connected to GitHub. Root Directory is `web`.
- **To deploy, push to `main`.** Don't run `vercel deploy` from inside `web/`: with the Root Directory set, the CLI would look for `web/web`. Until the Root Directory was set on 2026-10-31, the first deploy from a push served a 404.
- Production domain https://kawach-app.vercel.app. `kawach.vercel.app` was already taken.
- Pages: `/` (home) and `/privacy` (the privacy policy for the Play listing). The policy names the developer and shows the contact email 007mwnswrangwary@gmail.com.
- `/api/invite`, `/api/pair`, `/api/notify` and `/api/cleanup` (daily cron) are live. Functions run in `bom1` (Mumbai), next to Firestore.

## Next steps, in order

1. ~~Install and log in to the Firebase CLI~~: done.
2. ~~Confirm the backend~~: confirmed, Firebase + Vercel.
3. ~~Create the Firebase project and Android app~~: done (see Cloud setup above).
4. ~~Kawach milestone 2, family link~~: done.
5. ~~Vercel project and privacy policy~~: done.
6. ~~Milestone 3: screenshot check with on-device ML Kit OCR~~: done.
7. Milestone 4: weekly scam lessons, plus an admin panel on Vercel Hobby for editing patterns.
8. **Must-dos before the Play listing:**
   - **In-app "Delete my account".** Play requires it for apps with sign-in. Add a `/api/delete-account` endpoint that removes the user's doc, links, checks and auth user.
   - The listing's deletion URL is https://kawach-app.vercel.app/delete-account (live).
   - Tests for the Firestore rules, run on the Firebase emulator in CI.
9. Play Console: internal testing, then a closed test (12 testers for 14 days; recruit through AssamJobDesk), then production.
10. Blueprint roadmap from here:
   - **Months 3–4:** beta with 100 families.
   - **Months 5–8:** family plan at ₹49/month, then turn on the parked LLM check and voice; add call screening and Bengali/Bodo.
   - **Months 9–12:** bank or cooperative pilot, iOS, grants.
11. **Surokkha** field validation runs during Kawach's beta:
    - survey 200 women commuters
    - map 20 routes
    - meet the Guwahati police women's cell
    - Then build milestone 1, reusing Kawach's base.
