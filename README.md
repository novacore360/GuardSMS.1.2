# GuardSMS — Intelligent SMS Threat Detection

A professional Android app that analyzes incoming SMS messages for phishing, fraud, malware, and scam threats in real-time using community-powered threat intelligence.

---

## Features

- **Real-time SMS analysis** — Incoming messages are intercepted, analyzed, and classified instantly
- **Threat detection engine** — Keyword matching, pattern detection, domain reputation, URL shortener detection
- **Community threat database** — Flagged domains/URLs shared as hashes across all users
- **Red Flag button** — Users can manually flag messages; domains are auto-reported to the community list
- **Message preview** — Full message detail with threat breakdown
- **Contact sync** — Identifies senders not in your contacts ("Not in your contacts" warning)
- **Automatic deletion** — Messages deleted from Supabase after 24 hours
- **Report system** — Submit domains, URLs, or raw messages to the community
- **Privacy-first** — Messages/contacts stored raw; only URLs/domains/links are hashed
- **Sign in / Sign up** — Supabase Auth with email and password
- **Clean professional UI** — Tab navigation, status chips, stat cards, bottom sheets

---

## Setup

### 1. Supabase

1. Open your Supabase project at https://lrnvnbnnxuynaalggdtr.supabase.co
2. Go to **SQL Editor** → paste and run `supabase_schema.sql`
3. Enable **Email Auth** under Authentication → Providers
4. Copy your **anon/public key** from Settings → API

### 2. Environment Variables

In **Codemagic** → App Settings → Environment Variables → Group: `guardsms_env`, add:

| Variable | Value |
|---|---|
| `SUPABASE_URL` | `https://lrnvnbnnxuynaalggdtr.supabase.co` |
| `SUPABASE_ANON_KEY` | Your Supabase anon/public key |

### 3. Build with Codemagic

1. Connect this repo to Codemagic
2. Use the provided `codemagic.yaml`
3. Trigger a build — APK will be generated automatically

### 4. Local build

```bash
export SUPABASE_URL="https://lrnvnbnnxuynaalggdtr.supabase.co"
export SUPABASE_ANON_KEY="your_anon_key_here"
./gradlew assembleDebug
```

> **Note on the Gradle wrapper:** `gradle/wrapper/gradle-wrapper.jar` is a
> binary file that could not be regenerated in this environment (no network
> access). `gradlew` / `gradlew.bat` are included and correctly configured
> for Gradle 8.4, but you must generate the jar once:
>
> - **Easiest:** open the project in Android Studio — it detects the missing
>   wrapper jar and regenerates it automatically.
> - **Or run:** `./regenerate_wrapper.sh` on a machine with Gradle installed
>   (runs `gradle wrapper --gradle-version 8.4`).
> - **CI (Codemagic):** `codemagic.yaml` already regenerates it automatically
>   using the preinstalled Gradle before invoking `./gradlew`.
>
> Commit the resulting `gradle/wrapper/gradle-wrapper.jar` once generated.

---

## Build fixes applied

This project was reviewed and the following issues were fixed/added so it
builds cleanly:

- Added missing adaptive app icon resources (`mipmap-anydpi-v26/ic_launcher*`,
  `drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml`)
  referenced by `AndroidManifest.xml`.
- Added `gradlew` / `gradlew.bat` wrapper scripts (see note above about
  `gradle-wrapper.jar`).
- Fixed a Dagger/Hilt **duplicate binding** error: `AppModule` previously
  provided `GuardRepository` via `@Provides` *and* `GuardRepository` itself
  had `@Inject constructor()` + `@Singleton`, causing a compile-time
  "duplicate bindings" error in Dagger. The redundant `@Provides` was removed.
- `app/build.gradle` now resolves `SUPABASE_URL` / `SUPABASE_ANON_KEY` from
  either Gradle project properties (`-PSUPABASE_URL=...`, as passed by
  `codemagic.yaml`) or environment variables, with sensible fallbacks.
- `codemagic.yaml` now regenerates `gradle-wrapper.jar` automatically if
  missing and ensures `gradlew` is executable before building.

---

## Architecture

```
GuardSMS/
├── data/
│   ├── remote/         Supabase client
│   └── repository/     GuardRepository (all DB operations)
├── domain/
│   └── model/          Data classes (SmsMessage, FlaggedDomain, Contact…)
├── presentation/
│   ├── auth/           Login / SignUp / AuthViewModel
│   ├── dashboard/      Home stats + recent activity
│   ├── messages/       Message list with tabs (All/Flagged/Safe)
│   ├── reports/        Community flagged domains + user reports
│   ├── contacts/       Contact sync and list
│   ├── settings/       Account, privacy policy, version
│   └── common/         Shared adapters and dialogs
├── service/
│   ├── SmsReceiver     Broadcasts interceptor
│   ├── SmsAnalysisService  Foreground analysis + notifications
│   └── CleanupWorker   WorkManager 24h cleanup job
└── utils/
    ├── HashUtils        SHA-256 hashing + domain extraction
    ├── LinkExtractor    URL/domain extraction from SMS text
    └── SmsAnalyzer      Core threat detection engine
```

---

## Database Storage Policy

| Data | Storage | Retention |
|---|---|---|
| SMS message body | Raw text | **24 hours**, then auto-deleted |
| Sender phone number | Raw | 24 hours |
| Contact name + phone | Raw | Permanent (until account deletion) |
| URLs extracted from SMS | **SHA-256 hash only** | Permanent |
| Domains extracted from SMS | **SHA-256 hash only** | Permanent |
| Flagged domain | Raw domain + hash | Permanent |
| Report content | Raw text | Permanent |

---

## Privacy Policy

See `strings.xml` → `privacy_content` for the full in-app privacy policy text.

---

## Permissions Required

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` | Intercept incoming SMS for analysis |
| `READ_SMS` | Read SMS content |
| `READ_CONTACTS` | Identify if sender is in contacts |
| `INTERNET` | Sync with Supabase |
| `POST_NOTIFICATIONS` | Show threat alerts |
| `FOREGROUND_SERVICE` | Background analysis |
