# impels

Android reminder app for interrupted people. **Catch every ask. Set a timer you can't ignore.**

Catch a task in two taps, pick a time, and impels rings like a full-screen alarm (sound + vibration, over the lock screen) until you dismiss or snooze it. A one-shot timer, not a recurring nag.

This repo holds two things: the **marketing landing page** (deployed to Vercel) and the **build spec** for the Android app.

---

## For the coding agent (opencode + DeepSeek): READ THIS FIRST

**Your job:** build the impels Android app and produce a working debug APK.

1. **Read `impels-BUILD-SPEC.md` in full.** It is the single source of truth. Every decision
   (tech stack, versions, architecture, data model, screens, scheduling logic, permissions, theme,
   copy) is already made there. Do not invent features that are not in it.
2. **Build the Android project** in a new folder `android/` at the repo root, following the exact
   package structure in spec §4 and the Gradle setup in §3.
3. **Match the acceptance criteria in spec §16** — that is your definition of done. Verify each item.
4. **Output:** `./gradlew assembleDebug` must succeed →
   `android/app/build/outputs/apk/debug/app-debug.apk`.

Hard rules (from the spec, repeated so you don't miss them):
- **Offline-first. No network, no accounts, no cloud, no analytics.** (Cloud sync is v2.)
- Native Kotlin + Jetpack Compose (Material 3), MVVM, Room, AlarmManager. Manual DI (no Hilt/Koin).
- The core feature is the **repeat-nag loop**: each fired alarm reschedules the next one until the
  reminder is marked Done. See spec §7 — get this right.
- Dark theme only, amber `#F0A63A` accent. No serif fonts. One filled primary button per row;
  destructive actions are text-style in the danger color.
- Status: **beta**, versionName `0.1.0-beta`.

The landing page (`index.html`) shows exactly what the product should feel like — use it as the
visual and copy reference for the app UI and strings.

---

## Repo layout
```
index.html                 # landing page (Vercel deploys this at repo root)
shared/                    # landing CSS (tokens.css, shadcn.css)
assets/images/             # landing illustrations (webp)
assets/placeholders/       # image + video generation prompts (nanobanana, Google Flow)
impels-BUILD-SPEC.md    # THE spec for the Android app — build from this
PRE-LANDING.md             # early copy/structure notes for the landing page
STYLE-NOTES.md             # landing design system notes
android/                   # (to be created by the build agent) the Android app
```

## APK distribution
The app is **not built locally by users**. We publish the APK as a GitHub Release; the landing page
links to it:
`https://github.com/masjames/impels/releases/latest/download/impels.apk`

Build agent / maintainer: after building, attach the APK to a GitHub Release named `impels.apk`
so the download button on the site resolves.

## Landing page deploy (Vercel)
Static site, no build step. `index.html` is at the repo root, so Vercel needs **no configuration** —
import the GitHub repo in Vercel and deploy. Framework preset: **Other**. Build command: none.
Output directory: `.` (root).

## Local preview of the landing page
```
open index.html            # macOS, opens in browser
```

---
Built by [0xruzk](https://instagram.com/0xruzk).
