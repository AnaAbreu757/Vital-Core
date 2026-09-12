# VitalCore — Deployment Manual (GitHub, Firebase, Google Sign-In, Strava)

This manual covers everything needed to take the VitalCore project from this ZIP to a
working app on your phone with cloud login and third-party connections enabled, plus
publishing the code to GitHub. Read the honesty note below first — it explains what each
"connection" actually does before you spend time configuring it.

---

## 0. Honesty note — what's real, what isn't, and why

You asked for VitalCore to connect to Xiaomi Wear/Q Watch Pro, Samsung Health, Apple
Health, Mi Fit, and Strava, and to extract "all" health/fitness data from them. Here's
exactly what's possible and what VitalCore actually does:

| Source | What actually happens | Why |
|---|---|---|
| **Xiaomi Wear / Mi Fit / "Q Watch Pro"** | Already works — no setup. These apps sync into **Android Health Connect**, and VitalCore reads from Health Connect. | Xiaomi has no public third-party API for reading watch data directly; Health Connect *is* the sanctioned integration path on Android. |
| **Samsung Health** | Already works — no setup. Also flows through Health Connect. | Samsung's own Health Data SDK requires a partner agreement with Samsung for most scopes and isn't something a personal project can get approved for in the way "connect my account" implies. Health Connect is the real, working path. |
| **Strava** | Real OAuth2 connection, built and working. You register your own free Strava API application and paste two values into `local.properties`. | Strava publishes a genuine public REST API for exactly this. |
| **Apple Health** | File import only — no live connection. | There is no API, public or private, that lets an Android app read data from Apple Health. Apple doesn't expose one on either the iOS or Android side. The only honest option is exporting a file on the iPhone and importing it into VitalCore, which is what's built. |

If anyone (including a future AI assistant) offers to build a "direct Samsung Health
connection" or a "live Apple Health sync" beyond what's described above, be skeptical —
those specific capabilities do not exist as public APIs.

---

## 1. Prerequisites

- Android Studio Ladybug (2024.2)+, JDK 17, Android SDK 35 — same as `MANUAL.md`.
- A Google account (for Firebase + Google Sign-In).
- A free Strava account, if you want the Strava connection.
- A GitHub account, for publishing the repository.

---

## 2. Publishing to GitHub

```bash
unzip VitalCore-Complete.zip -d VitalCore
cd VitalCore
git init
git add .
git commit -m "feat: initial VitalCore project"
```

Create an empty repository on GitHub (github.com → New repository — **do not** initialize
it with a README, since you already have one), then:

```bash
git remote add origin https://github.com/<your-username>/<your-repo>.git
git branch -M main
git push -u origin main
```

**Before your first push**, double-check `local.properties` is *not* staged
(`git status` should not list it — it's already gitignored). If you ever accidentally
commit a secret, rotate it (regenerate the key in Firebase/Strava/Google Cloud) rather than
just deleting the commit, since it may already be cached by GitHub or forks.

CI (`.github/workflows/android.yml`) will run automatically on this push and on every
future push/PR to `main`. It builds fine with no Firebase/Strava configuration at all —
those features simply report "not configured" at runtime until you set them up below.

---

## 3. Firebase setup (Google Sign-In + cloud backup)

### 3.1 Create the Firebase project

1. Go to [console.firebase.google.com](https://console.firebase.google.com) → **Add
   project** → name it (e.g. "VitalCore") → follow the prompts (Google Analytics is
   optional, skip it if you don't want it).
2. Inside the project: **Build → Authentication → Get started → Sign-in method → Google →
   Enable**. Set a support email, save.
3. **Build → Firestore Database → Create database** → start in **production mode** →
   choose a region close to you.

### 3.2 Register the Android app

1. Project Overview → **Add app → Android**.
2. **Android package name:** `com.vitalcore.app` (must match exactly).
3. Debug signing certificate SHA-1: get it by running, from the project root:
   ```bash
   ./gradlew signingReport
   ```
   Copy the SHA-1 under the `debug` variant and paste it into the Firebase form. (Without
   this, Google Sign-In will fail even with correct keys — it's how Google verifies which
   app is asking.)
4. Download `google-services.json` **but you don't need to add it to the project** — this
   build reads Firebase config from `local.properties` instead (see below), which avoids
   requiring the Google Services Gradle plugin. Keep the downloaded file somewhere safe as
   a reference for the values in the next step, or open it in a text editor to copy values
   out of it.

### 3.3 Fill in `local.properties`

Copy `local.properties.example` to `local.properties` (already gitignored) and fill in:

```properties
FIREBASE_API_KEY=<"current_key" from google-services.json, under client > api_key > current_key>
FIREBASE_APP_ID=<"mobilesdk_app_id" from google-services.json>
FIREBASE_PROJECT_ID=<"project_id" from google-services.json>
FIREBASE_STORAGE_BUCKET=<"storage_bucket" from google-services.json>
```

### 3.4 Get the Google Sign-In Web Client ID

This is a *different* value from anything in `google-services.json` — it's the OAuth
client Firebase created automatically for web/server verification of the ID token.

1. Firebase Console → **Authentication → Sign-in method → Google** (click into it) →
   expand **Web SDK configuration** → copy the **Web client ID**.
2. Paste it into `local.properties`:
   ```properties
   GOOGLE_WEB_CLIENT_ID=<the Web client ID, ends in .apps.googleusercontent.com>
   ```

### 3.5 Firestore security rules

By default, a Firestore database created in "production mode" denies all access — you
need rules that let a signed-in user read/write only their own private data, while also
allowing the specific "public" documents that power Friends & Streaks (section 3.7) to be
read by any signed-in user. In Firebase Console → Firestore Database → **Rules**, replace
the contents with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Everything under a user's own document is private by default —
    // health scores, nutrition, friends list, app settings mirrored here.
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }

      // Exception: current/longest streak is intentionally public to any
      // signed-in VitalCore user, so a friend can look it up by uid. Only
      // the streak numbers live here — never health scores or nutrition.
      match /public_stats/{statId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null && request.auth.uid == userId;
      }
    }

    // Name + photo only, keyed by uid, readable by any signed-in user so a
    // friend code (= your uid) can be looked up and displayed.
    match /public_profiles/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Click **Publish**. This is what actually enforces "each user only sees their own private
data" — the app code scopes writes to `users/{uid}/...` but the *rule* is what makes that
mandatory rather than just convention. `public_profiles` and `public_stats` are the one
deliberate, documented exception — see section 3.7 for why.

### 3.6 Build and test

Rebuild the app (Android Studio → Sync, then Run). In Settings → Account & cloud backup,
"Sign in with Google" should now open the account picker and work. "Back up now" pushes
your scores to Firestore under `users/<your-uid>/scores/...` — you can watch it appear
live in Firebase Console → Firestore Database → Data.

---

### 3.7 Friends & Streaks — the honest privacy model

VitalCore shows a **streak** (consecutive days with health data logged) on a calendar, and
lets you add **friends** by sharing a code to see each other's streaks. Here's exactly what
that means and why it's built the way it is:

- **What's shared:** only your display name, profile photo, and current/longest streak
  count. Never Recovery/Sleep/Strain scores, never nutrition, never raw health data.
- **Who can see it:** *any signed-in VitalCore user who has your code* — not just people
  you've explicitly approved. Your "friend code" is simply your Firebase uid, shown on the
  Friends screen. Treat it like a username, not a password — share it only with people you
  actually want seeing your streak.
- **Why it's one-directional, not mutual "friend requests":** a proper mutual-friendship
  system (where both sides must approve, and each side's app can privately confirm the
  other accepted) needs a server component — typically a Cloud Function — to safely mirror
  a write across two different users' private data without opening a security hole. This
  project has no backend by design (see the rest of this manual: everything is
  client-configured). Rather than fake a "friend request" flow that isn't actually secure,
  VitalCore uses a simpler, honestly-labeled model: adding someone's code lets *you* see
  *their* public streak. If you want mutual visibility, both people add each other's code.
- **If you want true mutual/private friends later:** add a Cloud Function (Firebase's
  free tier covers light usage) that runs server-side, validates both users consented, and
  writes the mirrored `friends` entries with elevated privileges the client never has. This
  is a natural extension of the Firebase project you already set up above, not a rebuild.

## 4. Strava setup

### 4.1 Register a Strava API application

1. Log into [strava.com](https://www.strava.com) → Settings → **My API Application**
   (or go directly to [strava.com/settings/api](https://www.strava.com/settings/api)).
2. Fill in:
   - **Application Name:** VitalCore (or anything)
   - **Category:** Health and fitness
   - **Website:** anything valid, e.g. your GitHub repo URL
   - **Authorization Callback Domain:** `strava-callback` — **important:** Strava expects
     just the domain/host part here, and VitalCore's redirect URI is
     `vitalcore://strava-callback`, so the host is `strava-callback`.
3. Save. You'll see a **Client ID** and **Client Secret** on the resulting page.

### 4.2 Fill in `local.properties`

```properties
STRAVA_CLIENT_ID=<your numeric client ID>
STRAVA_CLIENT_SECRET=<your client secret>
```

### 4.3 Security note (read this)

Strava's OAuth2 token exchange requires the client secret to be sent in the request. For a
personal, single-user app installed only on your own device(s), embedding it in
`local.properties` → `BuildConfig` (never committed to git) is a reasonable simplification.
If you ever distribute this app publicly (Play Store, APK sharing, etc.), anyone could
decompile the APK and extract that secret. For that scenario, the correct fix is routing
the token exchange through a small backend you control (e.g. a Cloud Function) that holds
the secret server-side instead — a natural extension of the Firebase project you already
set up in section 3. This is called out as a known limitation rather than silently ignored.

### 4.4 Build and test

Rebuild. In Settings → Connections → Strava → **Connect Strava**, a browser tab opens for
Strava's login/authorize screen. After approving, it redirects back into VitalCore
automatically. **Sync now** pulls your recent activities (last 30 days by default) into
the same `exercise_sessions` table Health Connect data uses, so they immediately count
toward your Strain score.

---

## 5. Apple Health import

No account or API keys needed — see the honesty note in section 0 for why this is a file
import rather than a live connection.

1. On the iPhone: **Health app → tap your profile picture (top right) → Export All Health
   Data** → this produces a `.zip` containing `export.xml`.
2. `export.xml` is Apple's raw format, not CSV. Convert it using any "Apple Health XML to
   CSV" tool that preserves the `type` field exactly as Apple names it (e.g.
   `HKQuantityTypeIdentifierHeartRate`) — several free/open-source converters exist; search
   for "apple health export.xml to csv" and pick one that lets you choose the output
   columns, since VitalCore expects specifically `type,start,end,value,unit`.
3. Transfer the resulting `.csv` file to your Android device (email it to yourself, use
   Google Drive, USB transfer, etc.).
4. In VitalCore: Settings → Connections → Apple Health → **Import from file** → pick the
   CSV. You'll see a summary of rows read/imported, and any unrecognized `type` values so
   you know what wasn't imported.

Supported types in this MVP: heart rate, resting heart rate, HRV (SDNN), sleep analysis,
blood oxygen, respiratory rate, body mass. Extending `AppleHealthCsvImporter` with more
types (e.g. blood pressure, VO2 max) is a straightforward follow-up — the pattern for each
is a few lines in that file's `when` block.

---

## 6. Full first-run checklist

1. `git clone` your repo (or continue from the unzipped folder).
2. Copy `local.properties.example` → `local.properties`, fill in `sdk.dir` plus whichever
   of Firebase/Strava sections above you've completed (any subset is fine — each feature
   degrades gracefully when its values are blank).
3. Open in Android Studio, let Gradle sync (generates the wrapper JAR automatically).
4. Run on a device/emulator with Health Connect available.
5. Onboarding → pick a goal → Connect Health Connect (grant what you can).
6. Settings → sign in with Google (if configured) → connect Strava (if configured).
7. Health tab → Nutrition → log a meal and some water to see that flow working
   immediately, independent of any wearable.
8. Home tab → **Ask AI Coach** to confirm that flow (needs its own separate setup — see
   `MANUAL.md` "Configuration reference" for the AI Coach section, unrelated to Firebase).
9. Home tab → **View streak** to see today counted once you have data, and **Friends** to
   see your own friend code (only useful once signed in — see section 3.7).

---

## 7. Troubleshooting this manual specifically

**Google Sign-In shows "Signed-in as..." then instantly signs back out / fails silently.**
Almost always a SHA-1 mismatch. Re-run `./gradlew signingReport`, confirm the SHA-1 under
the `debug` variant exactly matches what's registered in Firebase Console → Project
settings → Your apps → Android app → SHA certificate fingerprints. Add it there if missing.

**"AI Coach" and "Google Sign-In" both say "not configured" even after editing
local.properties.**
Android Studio caches Gradle config — do **File → Sync Project with Gradle Files** (or a
full **Build → Clean Project** then rebuild) after any `local.properties` change, since
`BuildConfig` fields are generated at build time, not read live.

**Strava redirect opens the browser but never comes back to the app.**
Double check the **Authorization Callback Domain** in your Strava API application settings
is exactly `strava-callback` (no `vitalcore://` prefix — Strava's field only wants the
host). Also confirm you rebuilt after adding `STRAVA_CLIENT_ID`/`STRAVA_CLIENT_SECRET`.

**Firestore writes fail with a permission error.**
Your security rules (section 3.5) either weren't published or don't match — re-check
they're scoped to `users/{userId}/{document=**}` with the `request.auth.uid == userId`
condition, and that you're actually signed in when testing.

**I don't want cloud backup / Strava / any of this — can I just ignore this whole file?**
Yes. Every feature in this manual is additive and optional. An empty or missing
`local.properties` (aside from `sdk.dir`) leaves the entire rest of the app — Health
Connect, scoring, nutrition, notifications — fully functional exactly as described in
`MANUAL.md`.
