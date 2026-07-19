# intervval — Implementation-Ready Build Spec (Beta)

> One doc, end-to-end. Follow it top to bottom. Every decision is already made. Do not invent
> features not listed here. When in doubt, pick the simplest option that satisfies the acceptance
> criteria in §16. Target: a weaker model must be able to produce a working debug APK from this
> file alone.

---

## 0. What we are building (from the landing page)

intervval is an **Android reminder app for interrupted people**. Core promise (verbatim from the
landing page):

> Catch every ask. Stay focused. Get nagged until it's done.

Real situations it serves:
- A coworker asks for something while you're deep in another task.
- Your boss drops a task that must happen, but not right now.
- You tell yourself "I'll do it in a bit" and a bit becomes never.

The difference vs a to-do app: a normal to-do reminds **once**. intervval **keeps nagging on an
interval** (every X minutes) until you mark the item **done**.

Three product pillars:
1. **Catch it fast** — two-tap capture: what + who asked + how often to remind.
2. **Stay focused** — one active item shown big at the top, the rest wait quietly below.
3. **Get nagged** — a notification fires every X minutes until done; Snooze or Done from the
   notification itself.

---

## 1. Scope for this build (Beta)

IN SCOPE (must ship):
- Local, **offline-first**. No account, no login, no network calls at all.
- Create / edit / delete reminders.
- Repeat-nag scheduling with exact alarms.
- Notifications with **Done** and **Snooze** actions.
- Focus view (one active reminder highlighted, queue below).
- Reminders survive reboot (reschedule on boot).
- Permissions flow (notifications, exact alarm, battery-optimization hint).
- Settings (default interval, default snooze, theme is fixed dark — see §12).

OUT OF SCOPE (do NOT build — this is v2):
- Cloud sync, accounts, auth, sharing, multi-device.
- Any network/HTTP/Firebase/analytics SDK.
- Widgets, wear, tablet-specific layouts.
- Recurring calendar-style schedules (daily/weekly). Nagging here = repeat every X minutes until
  done, nothing more.

---

## 2. Tech stack (fixed — do not substitute)

- Language: **Kotlin 2.0.21**
- UI: **Jetpack Compose** (Material 3), single-Activity.
- Build: **Android Gradle Plugin 8.7.x**, Gradle 8.9.
- Min SDK **26** (Android 8.0), Target/Compile SDK **35**.
- Architecture: **MVVM**, single Gradle module (`:app`), unidirectional data flow with
  `StateFlow`.
- Persistence: **Room 2.6.1**.
- Preferences: **DataStore Preferences 1.1.1**.
- Background/scheduling: **AlarmManager** (exact alarms) + **BroadcastReceiver**. Use **WorkManager
  2.9.1** only for the boot-reschedule safety pass (optional). No periodic WorkManager for nagging.
- DI: **manual** (a single `AppContainer` created in `Application`). Do NOT add Hilt/Dagger/Koin.
- Async: Kotlin Coroutines + Flow (`kotlinx-coroutines-android`).
- Navigation: **Navigation-Compose 2.8.3**.

---

## 3. Gradle setup

### `gradle/libs.versions.toml`
```toml
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
coreKtx = "1.13.1"
lifecycle = "2.8.6"
activityCompose = "1.9.3"
composeBom = "2024.10.00"
navigation = "2.8.3"
room = "2.6.1"
datastore = "1.1.1"
coroutines = "1.9.0"
workmanager = "2.9.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-ui = { module = "androidx.compose.ui:ui" }
androidx-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-material3 = { module = "androidx.compose.material3:material3" }
androidx-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
workmanager = { module = "androidx.work:work-runtime-ktx", version.ref = "workmanager" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.25" }
```

### `app/build.gradle.kts` (key parts)
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
android {
    namespace = "app.intervval"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.intervval"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-beta"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildTypes { release { isMinifyEnabled = false } } // keep simple for beta
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.workmanager)
    debugImplementation(libs.androidx.ui.tooling)
}
```

---

## 4. Package / file structure
```
app/src/main/java/app/intervval/
  IntervvalApp.kt                 // Application, builds AppContainer, creates notif channel
  AppContainer.kt                 // manual DI: db, repo, scheduler, prefs
  MainActivity.kt                 // single activity, hosts NavHost, handles permissions

  data/
    ReminderEntity.kt             // Room @Entity
    ReminderDao.kt                // Room @Dao
    IntervvalDatabase.kt          // Room @Database
    SettingsStore.kt              // DataStore wrapper
    ReminderRepository.kt         // exposes Flows + suspend ops, calls scheduler

  domain/
    Reminder.kt                   // domain model + enums (Interval, ReminderStatus)

  schedule/
    ReminderScheduler.kt          // wraps AlarmManager (schedule/cancel/reschedule)
    AlarmReceiver.kt              // BroadcastReceiver: fires notification, schedules next
    BootReceiver.kt               // reschedules all active on BOOT_COMPLETED
    ActionReceiver.kt             // handles Done / Snooze notification actions
    Notifications.kt              // channel + notification builder helpers

  ui/
    theme/Theme.kt Color.kt Type.kt
    home/HomeScreen.kt HomeViewModel.kt
    edit/EditScreen.kt EditViewModel.kt
    settings/SettingsScreen.kt SettingsViewModel.kt
    components/…                  // ReminderCard, IntervalPicker, EmptyState, PermissionBanner
```

---

## 5. Domain model

`domain/Reminder.kt`
```kotlin
enum class ReminderStatus { ACTIVE, DONE }

// interval in minutes between nags
enum class IntervalOption(val minutes: Int, val label: String) {
    M5(5, "Every 5 min"), M10(10, "Every 10 min"), M15(15, "Every 15 min"),
    M30(30, "Every 30 min"), M60(60, "Every 1 hour");
    companion object { fun fromMinutes(m: Int) = entries.firstOrNull { it.minutes == m } ?: M15 }
}

data class Reminder(
    val id: Long = 0,
    val title: String,               // "what" — required, non-blank
    val fromWho: String? = null,     // "who asked" — optional (coworker, boss, me)
    val intervalMinutes: Int = 15,   // nag interval
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val isFocused: Boolean = false,  // the ONE highlighted item at top; at most one true
    val createdAt: Long = System.currentTimeMillis(),
    val nextTriggerAt: Long = 0L,    // epoch millis of next alarm
    val snoozedUntil: Long? = null   // if set and > now, nags paused until then
)
```

Rules:
- `title` required, trim, reject blank.
- At most **one** reminder has `isFocused = true`. Setting focus on one clears it on others.
- A new reminder is `ACTIVE`, `nextTriggerAt = now + intervalMinutes`.

---

## 6. Persistence (Room)

`data/ReminderEntity.kt` — mirror the domain fields; store enums as strings/int.
```kotlin
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fromWho: String?,
    val intervalMinutes: Int,
    val status: String,        // "ACTIVE" | "DONE"
    val isFocused: Boolean,
    val createdAt: Long,
    val nextTriggerAt: Long,
    val snoozedUntil: Long?
)
```

`data/ReminderDao.kt`
```kotlin
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status='ACTIVE' ORDER BY isFocused DESC, createdAt DESC")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status='DONE' ORDER BY createdAt DESC")
    fun observeDone(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id=:id") suspend fun getById(id: Long): ReminderEntity?
    @Query("SELECT * FROM reminders WHERE status='ACTIVE'") suspend fun getAllActive(): List<ReminderEntity>

    @Insert suspend fun insert(e: ReminderEntity): Long
    @Update suspend fun update(e: ReminderEntity)
    @Query("DELETE FROM reminders WHERE id=:id") suspend fun delete(id: Long)
    @Query("UPDATE reminders SET isFocused=0 WHERE isFocused=1") suspend fun clearFocus()
}
```

`data/IntervvalDatabase.kt` — `@Database(entities=[ReminderEntity::class], version=1)`, expose
`reminderDao()`. Build with `Room.databaseBuilder(...,"intervval.db").build()`.

`ReminderRepository` responsibilities:
- Map Entity <-> domain.
- Expose `observeActive(): Flow<List<Reminder>>`, `observeDone()`.
- `add(title, fromWho, intervalMinutes)`: insert with `nextTriggerAt=now+interval`, then
  `scheduler.schedule(reminder)`.
- `update(reminder)`: persist + reschedule (cancel + schedule).
- `markDone(id)`: set status=DONE, isFocused=false, `scheduler.cancel(id)`.
- `delete(id)`: `scheduler.cancel(id)`, DAO delete.
- `setFocus(id)`: `clearFocus()` then set that one focused.
- `snooze(id, minutes)`: `snoozedUntil=now+minutes`, `nextTriggerAt=now+minutes`, reschedule.
- `rescheduleAll()`: for boot — for every active reminder, recompute `nextTriggerAt` if in the past
  (`= now + interval`) and re-schedule.

---

## 7. Scheduling engine (the critical part — get this right)

### Behavior
- Each ACTIVE, non-snoozed reminder has exactly one pending exact alarm at `nextTriggerAt`.
- When the alarm fires (`AlarmReceiver`):
  1. Load the reminder. If missing / DONE → do nothing.
  2. If `snoozedUntil != null && snoozedUntil > now` → reschedule at `snoozedUntil`, return.
  3. Post/refresh the notification (see §8).
  4. Compute next: `nextTriggerAt = now + intervalMinutes*60_000`, persist, schedule next alarm.
     => This is what makes it **nag repeatedly** until Done.
- Marking **Done** or **Delete** cancels the alarm and cancels the notification.
- **Snooze** sets `snoozedUntil` / `nextTriggerAt = now + snoozeMinutes`, cancels current notif,
  reschedules.

### `ReminderScheduler.kt`
```kotlin
class ReminderScheduler(private val context: Context) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(id: Long, triggerAt: Long) {
        val pi = pendingIntent(id)
        // exact + allow while idle so it survives Doze; reminder app is allowed to use this
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi) // fallback inexact
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
    fun cancel(id: Long) = am.cancel(pendingIntent(id))

    private fun pendingIntent(id: Long): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            action = "app.intervval.NAG"; putExtra("id", id)
        }
        return PendingIntent.getBroadcast(
            context, id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
```

### `AlarmReceiver.kt` — do work off the main thread with `goAsync()`
```kotlin
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L); if (id < 0) return
        val pending = goAsync()
        val app = context.applicationContext as IntervvalApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = app.container.repository.getReminder(id) ?: return@launch
                if (r.status != ReminderStatus.ACTIVE) return@launch
                val now = System.currentTimeMillis()
                if (r.snoozedUntil != null && r.snoozedUntil > now) {
                    app.container.scheduler.schedule(id, r.snoozedUntil); return@launch
                }
                Notifications.showNag(context, r)
                val next = now + r.intervalMinutes * 60_000L
                app.container.repository.updateNextTrigger(id, next)
                app.container.scheduler.schedule(id, next)
            } finally { pending.finish() }
        }
    }
}
```

### `BootReceiver.kt`
- Registered for `RECEIVE_BOOT_COMPLETED` + `ACTION_MY_PACKAGE_REPLACED`.
- On receive: `repository.rescheduleAll()` (via goAsync + IO coroutine).

### `ActionReceiver.kt`
- Handles two actions from the notification: `ACTION_DONE`, `ACTION_SNOOZE` (extra `id`,
  snooze uses default snooze minutes from settings).
- DONE → `repository.markDone(id)` + `NotificationManagerCompat.cancel(id.toInt())`.
- SNOOZE → `repository.snooze(id, defaultSnoozeMinutes)` + cancel notification.

---

## 8. Notifications

`schedule/Notifications.kt`:
- Channel id `"nags"`, name "Reminders", **IMPORTANCE_HIGH**, create in `IntervvalApp.onCreate()`.
- `showNag(context, reminder)`:
  - Small icon: app timer icon.
  - Title = reminder.title. Text = `fromWho`?.let { "From $it" } ?: "Tap to open".
  - Priority HIGH, category `CATEGORY_REMINDER`, `setOnlyAlertOnce(false)` (each nag alerts),
    auto-cancel false.
  - Content intent → MainActivity deep link to that reminder.
  - Action 1: **Done** → ActionReceiver `ACTION_DONE`.
  - Action 2: **Snooze** → ActionReceiver `ACTION_SNOOZE`.
  - Notification id = `reminder.id.toInt()`.
- Respect `POST_NOTIFICATIONS` permission (API 33+): if not granted, skip posting.

---

## 9. Permissions

`AndroidManifest.xml` uses:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>          <!-- API 33+, reminder app -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>     <!-- API 31-32 -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
```
Register receivers in manifest (exported=false except Boot which needs the boot intent filter).

Runtime flow (in MainActivity / a first-run gate):
1. On first launch and when creating the first reminder, request `POST_NOTIFICATIONS` (API 33+).
2. If `!alarmManager.canScheduleExactAlarms()` (API 31+), show a banner "Turn on exact alarms so
   reminders fire on time" → open `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
3. Show a one-time dismissible banner suggesting to disable battery optimization
   (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — copy: "Some phones kill background apps. Allow
   intervval to run so nags don't die." Never block the app on this.

---

## 10. Screens (Compose, Material 3, dark theme)

Single Activity + `NavHost`. Routes: `home`, `edit?id={id}` (id optional; absent = new),
`settings`.

### 10.1 Home / Focus (`home`)
Layout top→bottom:
- **Top bar**: left = logo (timer icon + "intervval"), right = Settings icon button.
- **Permission banner** (only if exact-alarm or notif permission missing) — dismissible, amber.
- **Focused card** (if any reminder `isFocused`): large card, title big, `fromWho` chip, interval
  label, buttons: **Done** (primary), **Snooze**, and an unfocus/edit affordance.
- **Section label** "Up next" then the **queue**: list of other ACTIVE reminders as `ReminderCard`
  (compact): title, fromWho, interval; tap = edit; long-press or a star icon = set focus;
  swipe or overflow → Done / Delete.
- If zero active reminders → **EmptyState**: "Nothing on your plate. Catch the next ask." + a big
  Add button.
- **FAB** bottom-right: "+" opens Quick Add (navigate to `edit` with no id).
- Optional collapsed "Done" section at bottom: count + expandable list; each has "Restore".

### 10.2 Quick Add / Edit (`edit?id=`)
A bottom-sheet-style or full screen form. **Two-tap fast** capture:
- Field **What** (title) — autofocus, required.
- Field **Who asked** (fromWho) — optional; quick chips: `Boss`, `Coworker`, `Me` (tapping fills
  the field, still editable).
- **Interval** picker — segmented row of `IntervalOption` (default from settings, preselect 15m).
- Primary button **Save** (or "Catch it"). On new: also offer "Save & focus".
- If editing existing: show **Delete** as a link-style destructive action (red/orange), not a
  filled button.

### 10.3 Settings (`settings`)
- Default interval (IntervalOption).
- Default snooze minutes (options: 5, 10, 30).
- Button: "Fix exact-alarm permission" (opens system screen) — shown always.
- Button: "Battery optimization" (opens system screen).
- About: version `0.1.0-beta`, "by 0xruzk", link instagram.com/0xruzk.

### Navigation / deep link
Tapping a notification opens `home` and focuses/scrolls to that reminder (pass id via intent
extra, read in MainActivity, set it focused or open its edit — set it **focused** is preferred).

---

## 11. State management
- Each screen has a ViewModel exposing `StateFlow<UiState>`.
- `HomeViewModel`: combines `observeActive()` + `observeDone()` + permission state into
  `HomeUiState(focused: Reminder?, queue: List<Reminder>, done: List<Reminder>, needsPerms: Boolean)`.
- ViewModels get the repository from `AppContainer` via a simple `ViewModelProvider.Factory` (no DI
  lib). Use `viewModelScope` for suspend calls.

---

## 12. Theme (map the landing "High Accessibility" palette)

Dark, high-contrast, single amber signal. Fixed dark theme (no light mode in beta).
`ui/theme/Color.kt`:
```
val Bg        = Color(0xFF0D1014)
val Surface   = Color(0xFF141922)  // cards
val SurfaceAlt= Color(0xFF181C23)
val OnBg      = Color(0xFFF4F3EF)  // primary text (high contrast)
val Muted     = Color(0xFFB6BCC4)  // secondary text
val Border    = Color(0xFF303845)
val Primary   = Color(0xFFF0A63A)  // amber signal
val OnPrimary = Color(0xFF14110A)  // dark ink on amber
val Danger    = Color(0xFFFF8163)  // destructive actions (link-style, never filled)
```
Rules carried from the web design:
- **One primary (amber) filled button per action row.** Secondary actions = text/tonal, not filled.
- **Destructive actions** (Delete) = text button in `Danger`, never a filled button.
- Corner radius small (6–10dp). Typography: use the default Material 3 sans (no serif). Bold
  headings, generous spacing. Ensure text contrast passes WCAG AA on `Bg`.
- App icon / logo mark: a stopwatch circle with a lowercase "i" (dot + stem) inside; amber on dark.
  Provide as a vector drawable `ic_logo.xml` and use its silhouette for the notification small icon
  (`ic_stat_logo.xml`, monochrome).

---

## 13. Business rules (exhaustive)
1. Create: status ACTIVE, `nextTriggerAt = now + interval`, schedule alarm.
2. Nag fires every `intervalMinutes` until Done (receiver reschedules itself). See §7.
3. Snooze: pause nags until `now + snoozeMinutes`; then resume normal interval.
4. Done: status DONE, unfocus, cancel alarm + notification. Appears in Done list.
5. Restore (from Done): status ACTIVE, `nextTriggerAt = now + interval`, reschedule.
6. Delete: cancel alarm + notification, remove row. Confirm via a small dialog only for items with
   a title (avoid accidental loss).
7. Focus: exactly one focused item; setting focus clears others. Focused item sorts to top.
8. Editing interval reschedules using the new interval from now.
9. On boot / app update: reschedule all ACTIVE; if a `nextTriggerAt` is in the past, set it to
   `now + interval` (don't spam a burst of missed nags — fire at most the current one).
10. If notifications permission is denied, still schedule + still function; just can't post. Show
    the banner.

---

## 14. Edge cases
- Duplicate rapid Save taps → debounce Save button (disable while inserting).
- Very large number of reminders: list is lazy (`LazyColumn`). No pagination needed for beta.
- Blank title → block Save, show inline error.
- Alarm id: use reminder `id.toInt()` consistently for PendingIntent request code AND notification
  id so cancel works.
- Time zone / clock change: alarms are absolute epoch; acceptable for beta.
- Snooze while a notification is showing: cancel the current notification immediately.
- Reminder deleted while its notification is visible: ActionReceiver must null-check and just cancel
  the notification.

---

## 15. Copy (use these exact strings where they fit)
- App name: **intervval**
- Tagline: "Catch every ask. Get nagged until it's done."
- Empty state: "Nothing on your plate. Catch the next ask."
- Add button: "Get it down" / FAB content description "Add reminder".
- Notification Done action: "Done". Snooze action: "Snooze".
- Exact-alarm banner: "Turn on exact alarms so reminders fire on time."
- Battery banner: "Some phones kill background apps. Allow intervval to run so nags don't die."
- Settings About: "intervval 0.1.0-beta · by 0xruzk".
- No em-dashes in user-facing copy (use "·" or a period). No hype words.

---

## 16. Acceptance criteria (definition of done — verify each)
1. App builds a debug APK with `./gradlew assembleDebug` with zero errors.
2. Create a reminder with title + interval 5 min → within ~5 min a heads-up notification appears.
3. Leaving it untouched → it fires again roughly every 5 min (repeat nag confirmed at least twice).
4. Tapping **Done** on the notification → notification gone, no further nags, item moves to Done.
5. Tapping **Snooze** → no nag until snooze elapses, then nags resume.
6. Marking Done in-app cancels its scheduled alarm (no more notifications).
7. Setting focus on an item moves it to the big top card; only one item is ever focused.
8. Reboot the device/emulator → active reminders still nag afterwards (BootReceiver works).
9. Deny notification permission → app still runs; permission banner shows; scheduling still works.
10. Delete removes the item and cancels its alarm/notification.
11. No network permission in manifest; app works fully in airplane mode.
12. UI matches the dark amber theme in §12; destructive actions are text-style in Danger color.

---

## 17. Build & run
```
./gradlew assembleDebug            # output: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug             # to a connected device/emulator
```
Manual test: use an emulator with Google APIs, set nag interval to 5 min (shortest) to verify
repeat behavior quickly. For faster QA you MAY temporarily add a hidden 1-minute interval, but do
NOT ship it in the visible options.

---

## 18. Do-not-do reminders for the build model
- No cloud, no auth, no network libs, no analytics. Offline-first only.
- No Hilt/Koin. Manual `AppContainer`.
- No serif fonts. No filled destructive buttons. One amber primary per row.
- Don't add features beyond this spec. Ship the smallest thing that passes §16.
