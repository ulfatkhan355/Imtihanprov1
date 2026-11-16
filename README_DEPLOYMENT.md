# Imtihan Pro — Complete Ubuntu 26.04 Deployment & Testing Guide

This guide walks through setting up, testing, deploying, and connecting every component of **Imtihan Pro** (Stages 1–8) from an **Ubuntu 26.04** laptop:
- **Backend API**: Cloudflare Worker (`imtihan-pro-api`) + Cloudflare D1 (`imtihan-pro-db`) + Backblaze B2 (`imtihan-pro-assets`)
- **Web Admin Console**: React + TypeScript + Tailwind CSS (`admin/` -> Cloudflare Pages or local Vite)
- **Android Student App**: Kotlin + Jetpack Compose (`android/` -> physical Android phone via ADB or Emulator)

> **Important Security Note**: Never commit `.env`, `backend/.dev.vars`, release keystores, Gemini keys, or Backblaze/Cloudflare tokens to Git. All secrets stay in `.dev.vars` locally and `wrangler secret put` in Cloudflare.

---

## 1. Ubuntu 26.04 Prerequisites

Install Node.js 22 LTS, JDK 17, Android SDK Platform 35, Gradle 8.9+, and ADB:

```bash
# Node.js 22 LTS (if not installed)
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get update && sudo apt-get install -y nodejs openjdk-17-jdk adb unzip curl

# Verify versions
node -v        # v22.x
java -version  # OpenJDK 17
```

### Android SDK 35 + Gradle 8.9 (CLI or Android Studio)
If using **Android Studio (Ladybug or newer)**, install Android SDK Platform **API 35**, **Build-Tools 35.0.0**, and **Platform-Tools** via **SDK Manager**, then export:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

If generating the Gradle wrapper from CLI (`gradle` 8.9+):
```bash
cd android
gradle wrapper --gradle-version 8.9
chmod +x gradlew
```

---

## 2. Cloudflare CLI (`wrangler`) Setup & Login

Wrangler is listed as a devDependency inside `backend/package.json`. Install dependencies and authenticate with your Cloudflare account:

```bash
npm --prefix backend install
npx --prefix backend wrangler login
npx --prefix backend wrangler whoami
```

---

## 3. Existing Cloudflare & Backblaze B2 Resource Targets

`backend/wrangler.toml` is pre-bound to your existing infrastructure (do **not** recreate these resources):

- **Cloudflare Worker Name**: `imtihan-pro-api`
- **Cloudflare D1 Database**: `imtihan-pro-db` (`database_id = "079128c1-203b-477b-8c4b-d6b34daf8bbe"`)
- **Backblaze B2 Bucket**: `imtihan-pro-assets` (`B2_BUCKET_ID = "1bdf9ec6cd50dbdfad180110"`)
- **Backblaze B2 S3 Endpoint**: `https://s3.eu-central-003.backblazeb2.com` (`B2_REGION = "eu-central-003"`)

Verify the Worker can see D1 before deploying:
```bash
npx --prefix backend wrangler d1 list
```

---

## 4. Configuring Local & Production Secrets

### A. Local Development (`backend/.dev.vars`)
Copy the template and fill in your real values:
```bash
cp backend/.dev.vars.example backend/.dev.vars
```

Generate strong random secrets cleanly on Ubuntu:
```bash
openssl rand -hex 32   # use for ADMIN_SESSION_SECRET
openssl rand -hex 24   # use for ADMIN_BOOTSTRAP_SECRET
openssl rand -hex 24   # use for AGENT_INGEST_SECRET
```

In Backblaze B2 Console (**Application Keys**), create an application key scoped strictly to bucket `imtihan-pro-assets` (`readFiles`, `writeFiles`, `deleteFiles`, `listFiles`) and place `B2_KEY_ID` + `B2_APPLICATION_KEY` in `backend/.dev.vars`.

To enable the **server-side AI Tutor (`POST /v1/tutor/ask`)**, also set `GEMINI_API_KEY` (never shipped to Android or Admin clients; if omitted, `/v1/tutor/ask` returns HTTP `503 TUTOR_UNAVAILABLE` honestly).

### B. Production Worker Secrets (Cloudflare)
Set the exact same keys encrypted inside Cloudflare Workers:
```bash
cd backend
npx wrangler secret put ADMIN_SESSION_SECRET
npx wrangler secret put ADMIN_BOOTSTRAP_SECRET
npx wrangler secret put AGENT_INGEST_SECRET
npx wrangler secret put B2_KEY_ID
npx wrangler secret put B2_APPLICATION_KEY
# Optional for student Google Sign-In linking:
npx wrangler secret put GOOGLE_OAUTH_CLIENT_ID
# Optional for server-side grounded AI Tutor:
npx wrangler secret put GEMINI_API_KEY
```

---

## 5. Applying D1 Database Migrations (`0001`–`0004`) & KPK Seed

All migrations live in `backend/migrations/`:
- `0001_initial_schema.sql`: Core schema (16 tables)
- `0002_seed_kpk_sample.sql`: Idempotent KPK starter seed
- `0003_stage3_to_stage8.sql`: Class 9 KPK starter curriculum, BISE sub-boards, study groups, exam patterns, flashcards, mistakes, bookmarks, study planner, streaks/achievements, and feedback tables
- `0004_capacity_and_indexes.sql`: 15 composite D1 indexes, `daily_usage_metrics`, `ai_tutor_cache`, `ai_student_daily_usage`, and configurable `capacity_policy` thresholds

### Local D1 (for `wrangler dev` testing on laptop)
```bash
npm --prefix backend run d1:migrate:local
```

### Remote Production D1 (`imtihan-pro-db`)
```bash
npm --prefix backend run d1:migrate:remote
```

---

## 6. Running & Testing Locally on Ubuntu

Run the automated setup + unit/integration test suite (34 tests via `node:sqlite`):
```bash
chmod +x scripts/*.sh
./scripts/setup-local.sh
```

Run the isolated progressive load test (10–100 concurrent simulated students; never touches production) and review [`docs/FREE_TIER_CAPACITY_REPORT.md`](docs/FREE_TIER_CAPACITY_REPORT.md):
```bash
./scripts/run-load-test.sh
```

Start the local Cloudflare Worker listening on all interfaces (so both localhost Admin Dashboard and your Wi-Fi/USB Android phone can reach it):
```bash
npm --prefix backend run dev -- --ip 0.0.0.0 --port 8787
```

Verify health in another terminal:
```bash
curl -s http://localhost:8787/v1/health | jq .
```

Start the Admin Dashboard locally:
```bash
npm run dev
# Opens http://localhost:3000
```

---

## 7. Deploying the Backend (`imtihan-pro-api`)

```bash
./scripts/deploy-backend.sh
```
Wrangler outputs your live Worker URL (for example `https://imtihan-pro-api.<your-subdomain>.workers.dev`). Verify `/v1/health`:
```bash
curl -s https://imtihan-pro-api.<your-subdomain>.workers.dev/v1/health
```

---

## 8. Building & Deploying the Admin Dashboard

### Local Preview / Production Build
```bash
VITE_API_BASE_URL="https://imtihan-pro-api.<your-subdomain>.workers.dev/v1" ./scripts/build-admin.sh
```
Deploy `./dist` to **Cloudflare Pages**:
```bash
npx wrangler pages deploy dist --project-name=imtihan-pro-admin
```
> Remember to add your Pages URL (e.g. `https://imtihan-pro-admin.pages.dev`) to `ALLOWED_CORS_ORIGINS` in `backend/wrangler.toml` (comma-separated) and re-deploy the Worker.

---

## 9. First-Admin Bootstrap Workflow

1. Open the Admin Dashboard (`http://localhost:3000` or Cloudflare Pages URL).
2. Enter your Worker API Base URL (`http://localhost:8787/v1` locally or `https://imtihan-pro-api.<your-subdomain>.workers.dev/v1`).
3. When `GET /v1/admin/bootstrap-status` reports `hasAdmin: false`, the **Initialize First Super Admin** form appears.
4. Enter your `ADMIN_BOOTSTRAP_SECRET`, admin email, display name, and a password (`>= 10` characters).
5. Once initialized, `/v1/admin/bootstrap` locks permanently (`409 ADMIN_ALREADY_INITIALIZED`); future sign-ins use `/v1/admin/login`.

---

## 10. Building, Unit-Testing & Installing the Android Debug APK

Make sure `ANDROID_HOME` points to your SDK 35 folder and `JAVA_HOME` points to JDK 17:

```bash
# Option A: Build pointing to local Ubuntu laptop IP (for Wi-Fi/LAN testing on physical phone)
LAPTOP_IP=$(hostname -I | awk '{print $1}')
IMTIHAN_API_BASE_URL="http://${LAPTOP_IP}:8787/v1" ./scripts/build-android-debug.sh

# Option B: Build pointing to deployed Cloudflare Worker
IMTIHAN_API_BASE_URL="https://imtihan-pro-api.<your-subdomain>.workers.dev/v1" ./scripts/build-android-debug.sh
```

`build-android-debug.sh` automatically runs `./gradlew testDebugUnitTest` (Stage 1–8 Kotlin unit tests including `Stage3To8EngineTests`) and builds `android/app/build/outputs/apk/debug/app-debug.apk`.

Enable **Developer Options -> USB Debugging** on your Android phone, connect via USB, and install:
```bash
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```
*(You can also change the Worker API URL anytime inside the Android app under **Profile -> Worker URL** without rebuilding the APK!)*

---

## 11. End-to-End Physical Phone Verification Checklist (Stages 1–8)

1. **Class 9 KPK Guest-First Onboarding**: Open Imtihan Pro on the phone, select **Class 9**, a **KPK BISE Sub-Board** (e.g., **BISE Peshawar**), **Study Group** (`Science (Biology)` or `Science (Computer)`), and **Medium** (`English` / `Urdu`), then tap **Continue as Guest** -> Home Dashboard loads immediately with zero login wall.
2. **Real Home Metrics & Multi-Entity Search**: Verify Home Dashboard displays real computed progress, streak days, and daily study bar chart (never hardcoded scores), and search across subjects/chapters/PDFs.
3. **Notes Reader, Bookmarks & Worked Examples**: Open **Subjects -> Physics (Class 9) -> Chapter 1 (Physical Quantities and Measurement)** -> verify Detailed Notes, Worked Examples, Solved Questions, Chapter Bookmark toggle, and offline reading via Room.
4. **Mock Exams, Question Palette & Zero Ads**: Open **Practice -> Start KPK Board Mock / Chapter Quiz** -> verify timed countdown, Question Palette jump, negative marking badge, subjective Section B/C preview, **zero ads** inside quiz/reader, and Topic Accuracy Breakdown on results.
5. **Smart Insights, Mistake Master & SM-2 Flashcards**: Verify **Smart Insights** displays `"Not enough data yet"` when `< 2` quizzes exist and calculates real readiness + weak chapters once `>= 2` quizzes are completed; test **Mistake Master** retry and **Flashcards** SM-2 intervals.
6. **Study Planner, Motivation & Opt-In Leaderboard**: Set daily study target + board countdown in **Study Planner**, verify XP & streak updates in **Motivation**, and confirm leaderboard remains private unless explicitly opted in.
7. **Server-Side AI Tutor & Student Feedback**: Ask a chapter-grounded question in **AI Tutor** (verifying `503 TUTOR_UNAVAILABLE` if `GEMINI_API_KEY` isn't set, or a grounded answer when configured) and submit an issue in **Report Issue** -> verify it appears under **Admin Console -> Student Feedback**.
8. **Backblaze B2 PDF Upload -> Android `PdfRenderer` Reader**:
   - In **Admin Console -> PDF Library**, upload a `.pdf` with a category (`Past Paper`, `Textbook`, `Notes Pack`, `Syllabus`).
   - Verify `imtihan-pro-assets` B2 key in **Admin Console -> Infrastructure** and open it in **Papers** on Android via native `PdfRenderer` (with **zero ads** in `PdfReaderScreen`).

---

## 12. Optional Google Sign-In Configuration

- Create an Android OAuth client (matching SHA-1 of your debug/release keystore) and a **Web Application OAuth Client ID** in Google Cloud Console.
- Store the **Web Client ID** as `GOOGLE_OAUTH_CLIENT_ID` on the Cloudflare Worker (`wrangler secret put GOOGLE_OAUTH_CLIENT_ID`).
- In **Profile -> Link Google Account**, `POST /v1/auth/google/exchange` validates the Google ID token via `oauth2.googleapis.com/tokeninfo` (`aud` + `exp` + `email_verified`) and merges local guest progress monotonically into the Google-linked D1 student record.

---

## 13. AI Agent Content Ingestion Test

Send an AI draft submission (`status` is always forced to `'queued'`; agents can never self-publish):
```bash
curl -X POST http://localhost:8787/v1/agent/submissions \
  -H "Content-Type: application/json" \
  -H "X-Agent-Secret: YOUR_AGENT_INGEST_SECRET" \
  -d '{
    "agentName": "curriculum-drafter",
    "modelIdentifier": "gemini-2.5-pro",
    "boardCode": "KPK",
    "targetClass": 9,
    "subjectId": "sub_kpk9_phy",
    "chapterTitle": "Kinematics",
    "submissionType": "detailed_notes",
    "blocks": [
      { "id": "b1", "orderIndex": 1, "type": "heading", "sectionCode": "2.1", "text": "Rest and Motion", "isSubheading": false },
      { "id": "b2", "orderIndex": 2, "type": "paragraph", "text": "A body is said to be at rest if it does not change its position with respect to its surroundings." }
    ]
  }'
```
Then approve and promote it through the 4-stage workflow (`draft -> in_review -> approved -> published`) inside **Admin Console -> AI Review Queue / Content Studio**.

---

## 14. Troubleshooting

- **Phone can't reach `http://<ubuntu-ip>:8787/v1`**: Ensure Wrangler was started with `--ip 0.0.0.0`, check Ubuntu UFW (`sudo ufw allow 8787/tcp`), or use USB reverse port forwarding (`adb reverse tcp:8787 tcp:8787` and set Android Worker URL to `http://127.0.0.1:8787/v1`).
- **CORS error in Admin Dashboard**: Add your Admin origin (e.g. `http://localhost:3000` or `https://imtihan-pro-admin.pages.dev`) to `ALLOWED_CORS_ORIGINS` in `backend/wrangler.toml`.
- **Backblaze B2 `503 B2_NOT_CONFIGURED` or `403`**: Verify `B2_KEY_ID` and `B2_APPLICATION_KEY` are set in `backend/.dev.vars` (local) or `wrangler secret put` (remote), scoped to `imtihan-pro-assets` in region `eu-central-003`.
- **AI Tutor returns `503 TUTOR_UNAVAILABLE`**: Expected when `GEMINI_API_KEY` is omitted from `backend/.dev.vars` or Worker secrets. Add a valid key to enable live server-side explanations.
- **Android `./gradlew` fails with Java version error**: Ensure `java -version` reports **17** (`sudo update-alternatives --config java`).
