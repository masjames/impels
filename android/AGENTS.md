# AGENTS.md — intervval Android

You are working on the **intervval** Android app (offline-first reminder app that nags on an
interval until a task is marked done). Read `../intervval-BUILD-SPEC.md` for the full spec and
`../index.html` for the visual/copy reference.

## State of the code
Claude built the **skeleton + the hard core**, which you MUST NOT rewrite or change the public API of:
- Gradle setup (`build.gradle.kts`, `libs.versions.toml`) — do not bump versions.
- `data/` (Room, repository, DataStore) — stable.
- `schedule/` (AlarmManager, receivers, notifications) — **the nag engine. Do not touch logic.**
- ViewModels in `ui/home`, `ui/edit`, `ui/settings` — keep their public API (state + functions).
- `ui/theme/` — palette is locked to spec §12.

## Your job (UI polish only)
Elevate the Compose screens to match spec §10 and the landing page look, WITHOUT changing any
ViewModel API or the schedule/data layers:
- `ui/home/HomeScreen.kt` — make the focused card large and calm; nicer "Up next" list; a
  collapsible Done section; a real empty state. Keep using `HomeViewModel` as-is.
- `ui/edit/EditScreen.kt` — two-tap-fast capture; who-chips; interval chips; bottom-sheet feel.
- `ui/settings/SettingsScreen.kt` — tidy sections; keep the system-screen buttons working.
- Add small reusable composables under `ui/components/` if helpful.
- Optionally add a dismissible permission banner on Home (exact-alarm / notifications missing).

## Hard rules (from spec, do not break)
- Offline only. No network, no new dependencies without asking.
- Dark theme, amber `#F0A63A` accent, no serif fonts.
- One filled primary button per action row; secondary actions are text buttons; destructive
  actions (Delete) are text buttons in the error/danger color, never filled.
- No em-dashes or hype words in user-facing copy. Use the strings in spec §15.

## Definition of done
- `./gradlew assembleDebug` compiles with zero errors.
- All acceptance criteria in spec §16 still hold (especially: repeat-nag works, Done/Snooze from
  notification work, reboot reschedules).
