#!/usr/bin/env bash
set -uo pipefail
# impels — headless screenshot + video capture (Intel Mac, no GUI)

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTDIR="$REPO_DIR/captures"
APK="$REPO_DIR/impels.apk"
AVD_NAME="fresh"
SDK_DIR="/Users/mm/Library/Android/sdk"
PIDFILE="/tmp/emu-impels.pid"
JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$SDK_DIR/platform-tools:$SDK_DIR/emulator:$SDK_DIR/cmdline-tools/latest/bin:$PATH"
export JAVA_HOME
mkdir -p "$OUTDIR"

# gRPC capture helper (adb screencap returns black here — see HANDOFF.md)
PY="$REPO_DIR/.capture/venv/bin/python"
GRPC="$REPO_DIR/grpc_capture.py"

say() { printf "\n━━━ %s ━━━\n" "$*"; }

# ── Boot emulator ─────────────────────────────────────────────
boot_emu() {
  say "Killing previous emulator…"
  adb emu kill 2>/dev/null || true; sleep 2
  pkill -f "qemu-system" 2>/dev/null || true; sleep 1

  say "Booting '$AVD_NAME' headless (host GPU + gRPC)…"
  # gRPC getScreenshot reads the emulator framebuffer directly, bypassing the
  # black HWC layer that adb screencap sees — so we DON'T need software GL.
  # Use -gpu host (Mac GPU): fast rendering avoids the ANRs that swiftshader's
  # slow software GL caused (which backgrounded the app mid-capture).
  "$SDK_DIR/emulator/emulator" \
    -avd "$AVD_NAME" \
    -no-window -no-audio -no-snapshot \
    -gpu host -grpc 8554 \
    -netdelay none -netspeed full \
    > /tmp/emu-impels.log 2>&1 &
  echo $! > "$PIDFILE"

  # Wait for adb device
  local i=0
  while ! adb get-state 2>/dev/null | grep -q device; do
    sleep 5; ((i+=5))
    [[ $((i % 60)) -eq 0 ]] && echo "  waiting for device (${i}s)…"
    [[ $i -gt 600 ]] && { echo "FATAL: device timeout"; exit 1; }
  done

  # Wait for boot completed
  i=0
  while [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; do
    sleep 5; ((i+=5))
    [[ $((i % 60)) -eq 0 ]] && echo "  waiting for boot (${i}s)…"
    [[ $i -gt 600 ]] && { echo "FATAL: boot timeout"; exit 1; }
  done

  # Wait for input service to be available
  i=0
  while ! adb shell input keyevent KEYCODE_HOME 2>/dev/null; do
    sleep 3; ((i+=3))
    [[ $((i % 30)) -eq 0 ]] && echo "  waiting for input service (${i}s)…"
    [[ $i -gt 180 ]] && { echo "  input service not ready, continuing…"; break; }
  done

  # Wake + unlock screen
  adb shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 1
  adb shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 1
  # Swipe up to dismiss keyguard
  adb shell input touchscreen swipe 500 2000 500 500 200 2>/dev/null || true
  sleep 2
  # Press home to make sure we're at launcher
  adb shell input keyevent KEYCODE_HOME 2>/dev/null || true
  sleep 1

  # ── Headless input stability ──────────────────────────────────
  # 1) Suppress ANR/crash dialogs: SystemUI/Wellbeing ANRs otherwise steal
  #    window focus and make uiautomator dump return an empty hierarchy.
  adb shell settings put global hide_error_dialogs 1 2>/dev/null || true
  adb shell pm disable-user --user 0 com.google.android.apps.wellbeing 2>/dev/null || true
  # 2) Disable the soft keyboard. `adb input text` still injects into the
  #    focused field, but no IME means it can't occlude the Save buttons.
  for ime in $(adb shell ime list -s 2>/dev/null | tr -d '\r'); do
    adb shell ime disable "$ime" 2>/dev/null || true
  done
  echo "  system ready (~${i}s input wait)"
}

# ── Helpers ───────────────────────────────────────────────────
# Dismiss any "… isn't responding" ANR dialog (taps "Wait").
anr_dismiss() {
  if adb shell dumpsys window 2>/dev/null | grep -qi "isn't responding\|Application Not Responding"; then
    adb shell input tap 540 1400 2>/dev/null || true   # "Wait"
    sleep 0.5
  fi
}

# Ensure impels is the foreground app before capturing.
app_foreground() {
  adb shell dumpsys activity activities 2>/dev/null | grep -q "ResumedActivity.*app.impels" && return 0
  adb shell am start -n app.impels/.MainActivity 2>/dev/null || true
  sleep 2
}

snap() {
  local name="$1"; sleep 1.5
  anr_dismiss
  "$PY" "$GRPC" snap "$OUTDIR/$name.png" 2>/dev/null
  local sz=$(wc -c < "$OUTDIR/$name.png" | tr -d ' ' 2>/dev/null)
  echo "  -> $OUTDIR/$name.png (${sz} bytes)"
}

_type() {
  local t="${1// /%s}"
  adb shell input text "$t" 2>/dev/null; sleep 0.5
}

_find_xy() {
  local target="$1" mode="${2:-text}"
  adb shell uiautomator dump /sdcard/ui.xml 2>/dev/null || true
  adb pull /sdcard/ui.xml /tmp/_ui.xml 2>/dev/null || true
  [[ -s /tmp/_ui.xml ]] || { echo "NF"; return; }
  python3 -c "
import xml.etree.ElementTree as ET, sys
try:
    attr = {'desc':'content-desc','id':'resource-id','text':'text'}.get('$mode','text')
    for el in ET.parse('/tmp/_ui.xml').getroot().iter('node'):
        val = el.get(attr, '') or ''
        # resource-id matches on the trailing testTag (…:id/<tag> or bare <tag>)
        if '$mode' == 'id':
            val = val.rsplit('/', 1)[-1]
        if ('$mode' == 'id' and val == '$target') or ('$mode' != 'id' and '$target' in val):
            b = el.get('bounds','')
            if b:
                p = b.replace('[',',').replace(']',',').split(',')
                x = (int(p[1])+int(p[3]))//2
                y = (int(p[2])+int(p[4]))//2
                print(f'{x} {y}')
                sys.exit(0)
    print('NF')
except: print('ERR')
" 2>/dev/null || echo "ERR"
}

_tap() {
  local xy; xy=$(_find_xy "$1" "${2:-text}")
  [[ "$xy" == "NF" || "$xy" == "ERR" ]] && return 1
  local x="${xy% *}" y="${xy#* }"
  [[ "$x" =~ ^[0-9]+$ && "$y" =~ ^[0-9]+$ ]] || return 1
  # Retry tap if input service not ready
  local attempt=0
  until adb shell input tap $x $y 2>/dev/null; do
    ((attempt++)); [[ $attempt -ge 5 ]] && return 1
    sleep 3
  done
  sleep 0.8
}

# Tap a Compose control by its testTag (exposed as resource-id via
# testTagsAsResourceId). Reliable vs. text matching. Falls back cleanly.
_tap_id() { _tap "$1" id; }

go_back() { adb shell input keyevent KEYCODE_BACK 2>/dev/null; sleep 1.2; }

unlock() {
  adb shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 0.5
  adb shell input touchscreen swipe 500 2000 500 500 200 2>/dev/null || true
  sleep 1
}

ensure_home() {
  unlock
  anr_dismiss
  # Cold-restart so we always land on the home destination (reminders persist in
  # Room, so focused/queue/done state carries over — only the nav stack resets).
  adb shell am force-stop app.impels 2>/dev/null || true
  sleep 1
  adb shell am start -n app.impels/.MainActivity 2>/dev/null || true
  sleep 3
  # Wait until the app's splash/UI appears
  local i=0
  while ! adb shell dumpsys window windows 2>/dev/null | grep -qi "app.impels"; do
    sleep 2; ((i+=2))
    [[ $i -gt 20 ]] && break
  done
  sleep 1
}

# ── 1. Boot ───────────────────────────────────────────────────
boot_emu
echo "Model: $(adb shell getprop ro.product.model | tr -d '\r')"
echo "Size:  $(adb shell wm size | tr -d '\r')"

# ── 2. Install & setup ────────────────────────────────────────
say "Installing APK…"
adb install -r "$APK" 2>&1 | tail -1
# Wipe app data so the AVD's persisted reminders don't pollute the empty-home shot.
adb shell pm clear app.impels 2>/dev/null || true

say "Dismissing setup wizard (if any)…"
adb shell pm disable com.google.android.setupwizard 2>/dev/null || true
adb shell pm disable com.android.setupwizard 2>/dev/null || true
adb shell input keyevent KEYCODE_HOME 2>/dev/null || true
sleep 2
# Launch app and give it time to render fully
adb shell am start -n app.impels/.MainActivity 2>/dev/null || true
sleep 8

say "Clean status bar…"
adb shell settings put global sysui_demo_allowed 1 2>/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0940 2>/dev/null || true
sleep 0.5
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false 2>/dev/null || true
sleep 0.5
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false 2>/dev/null || true
sleep 1
adb shell pm grant app.impels android.permission.POST_NOTIFICATIONS 2>/dev/null || true

# ── 3. Capture screens ────────────────────────────────────────
# Open the add/edit form (FAB, or the empty-state button when the list is empty).
open_form() {
  _tap_id fab_add || _tap_id empty_add || { adb shell input tap 900 2200; sleep 1.5; }
  sleep 1.2
}

say "1/8 — Empty home"
ensure_home
snap "home-empty"

say "2/8 — Add form (blank)"
open_form
snap "add-form"

say "3/8 — Add form (filled)"
_tap_id field_what || adb shell input tap 540 400; sleep 0.4
_type "Review Q3 report"
_tap_id chip_who_Boss || true
_tap_id chip_int_30 || true
snap "add-form-filled"
go_back

say "4/8 — Focused card (Save & focus)"
ensure_home
open_form
_tap_id field_what || adb shell input tap 540 400; sleep 0.4
_type "Review Q3 report"
_tap_id chip_who_Boss || true
_tap_id chip_int_10 || true
_tap_id btn_save_focus || true; sleep 3
snap "home-focused"

say "5/8 — Queue (second reminder via Catch it)"
ensure_home
open_form
_tap_id field_what || adb shell input tap 540 400; sleep 0.4
_type "Order team lunch"
_tap_id chip_who_Coworker || true
_tap_id chip_int_60 || true
_tap_id btn_save || true; sleep 2
snap "home-queue"

say "6/8 — Done"
ensure_home
_tap_id btn_done || true; sleep 2
snap "home-done"

say "7/8 — Settings"
ensure_home
_tap_id btn_settings || _tap "Settings" desc || true
sleep 1.5; snap "settings"
go_back; sleep 1

say "8/8 — Alarm screen"
adb shell am start -n app.impels/.schedule.AlarmActivity \
  --el id 999 --es title "Review Q3 report" --es who "Boss" 2>/dev/null
sleep 2.5; snap "alarm-screen"
_tap_id btn_dismiss || true; sleep 1

# ── 4. Video ──────────────────────────────────────────────────
say "Video walkthrough (45 sec)"
adb shell pm clear app.impels 2>/dev/null || true; sleep 2
ensure_home

# gRPC frame recorder (screenrecord also yields black). Polls framebuffer for
# 48s at 10fps into PNGs; ffmpeg assembles them into the demo MP4 afterward.
FRAMEDIR="$OUTDIR/_frames"
"$PY" "$GRPC" record "$FRAMEDIR" 48 10 &
REC_PID=$!; sleep 4

say "Recording…"
sleep 3
open_form
sleep 1
_tap_id field_what || adb shell input tap 540 400; sleep 0.5
_type "Review Q3 report"
_tap_id chip_who_Boss || true
_tap_id chip_int_15 || true; sleep 1
_tap_id btn_save_focus || true; sleep 3

ensure_home
open_form
sleep 1
_tap_id field_what || adb shell input tap 540 400; sleep 0.5
_type "Fix login bug"
_tap_id chip_who_Coworker || true
_tap_id btn_save || true; sleep 3
_tap_id btn_done || true; sleep 3
ensure_home
_tap_id btn_settings || true; sleep 3
go_back; sleep 2

wait $REC_PID 2>/dev/null || true; sleep 1
say "Encoding demo.mp4 from frames…"
IN_FPS=$(cat "$FRAMEDIR/fps.txt" 2>/dev/null || echo 2)
# Encode real-time: input fps = actual capture rate, so length ≈ walkthrough
# wall-clock (~45s). Duplicate frames up to 30fps for smoother playback
# (mi_mode=dup is cheap and reliable; blend/mci can OOM on 1080x2400 frames).
if ffmpeg -y -framerate "$IN_FPS" -i "$FRAMEDIR/frame-%05d.png" \
    -vf "minterpolate=fps=30:mi_mode=dup,scale=1080:2400" \
    -c:v libx264 -pix_fmt yuv420p -crf 20 "$OUTDIR/demo.mp4" 2>/dev/null; then
  rm -rf "$FRAMEDIR"          # only delete frames once encode succeeds
else
  echo "  interpolated encode failed — falling back to plain encode"
  ffmpeg -y -framerate "$IN_FPS" -i "$FRAMEDIR/frame-%05d.png" \
    -c:v libx264 -pix_fmt yuv420p -crf 20 "$OUTDIR/demo.mp4" 2>/dev/null \
    && rm -rf "$FRAMEDIR"
fi
echo "  -> $OUTDIR/demo.mp4"

# ── 5. Cleanup ────────────────────────────────────────────────
say "Shutting down…"
adb emu kill 2>/dev/null || true
rm -f "$PIDFILE"
say "Output:"
ls -lh "$OUTDIR"/
