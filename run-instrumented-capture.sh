#!/usr/bin/env bash
set -uo pipefail
# impels — headless capture driven by an instrumented Compose UI test.
#
# Why: adb-driven navigation on this emulator is defeated by splash-tap loss,
# the soft keyboard occluding buttons, and flaky uiautomator (see HANDOFF.md).
# The androidTest drives the app IN-PROCESS via testTags (deterministic), and
# signals this script to take each screenshot via the emulator gRPC API — the
# only capture path that isn't black on this headless Intel-Mac emulator.

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTDIR="$REPO_DIR/captures"
SDK_DIR="/Users/mm/Library/Android/sdk"
AVD_NAME="fresh"
JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export JAVA_HOME
export PATH="$SDK_DIR/platform-tools:$SDK_DIR/emulator:$PATH"
PY="$REPO_DIR/.capture/venv/bin/python"
GRPC="$REPO_DIR/grpc_capture.py"
APP_APK="$REPO_DIR/android/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$REPO_DIR/android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
SIGDIR="/sdcard/Android/data/app.impels/files/capture"
RUNNER="app.impels.test/androidx.test.runner.AndroidJUnitRunner"
# Pin Gradle 8.11.1 — AGP 8.7.2 doesn't support Gradle 9.x (the wrapper jar is
# missing so we invoke a cached distribution's binary directly).
GRADLE="$(ls ~/.gradle/wrapper/dists/gradle-8.11.1-bin/*/gradle-8.11.1/bin/gradle 2>/dev/null | head -1)"
mkdir -p "$OUTDIR"
say() { printf "\n━━━ %s ━━━\n" "$*"; }

# ── 1. Build app + test APKs ──────────────────────────────────
say "Building app + androidTest APKs…"
( cd "$REPO_DIR/android" && "$GRADLE" :app:assembleDebug :app:assembleDebugAndroidTest ) \
  2>&1 | tail -3
[[ -f "$APP_APK" && -f "$TEST_APK" ]] || { echo "FATAL: APK build failed"; exit 1; }

# ── 2. Boot emulator (host GPU + gRPC) ────────────────────────
say "Booting '$AVD_NAME' headless…"
adb emu kill 2>/dev/null || true; sleep 2; pkill -f qemu-system 2>/dev/null || true; sleep 1
"$SDK_DIR/emulator/emulator" -avd "$AVD_NAME" \
  -no-window -no-audio -no-snapshot -gpu host -grpc 8554 \
  -netdelay none -netspeed full > /tmp/emu-impels.log 2>&1 &

i=0
until adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q 1; do
  sleep 5; ((i+=5)); [[ $((i%60)) -eq 0 ]] && echo "  booting (${i}s)…"
  [[ $i -gt 600 ]] && { echo "FATAL: boot timeout"; exit 1; }
done
adb shell settings put global hide_error_dialogs 1 2>/dev/null || true
echo "  booted (${i}s)"

# ── 3. Install & prepare ──────────────────────────────────────
say "Installing app + test APKs…"
adb install -r -g "$APP_APK"  2>&1 | tail -1
adb install -r    "$TEST_APK" 2>&1 | tail -1
adb shell pm clear app.impels 2>/dev/null || true
adb shell pm grant app.impels android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell settings put global sysui_demo_allowed 1 2>/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0940 2>/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false 2>/dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false 2>/dev/null || true
adb shell mkdir -p "$SIGDIR" 2>/dev/null || true
adb shell rm -f "$SIGDIR"/* 2>/dev/null || true

# AOT-compile app + test to `speed`. Without this the process runs interpreted
# under instrumentation and is so slow on this emulator that it misses the
# bind-application timeout → "failed to complete startup" ANR → "Process crashed".
# (Also: AVD needs ≥4 GB RAM — set hw.ramSize=4096 in ~/.android/avd/fresh.avd/config.ini.)
say "Warming + AOT-compiling app + test…"
adb shell am start -n app.impels/.MainActivity >/dev/null 2>&1 || true; sleep 8
adb shell am force-stop app.impels 2>/dev/null || true; sleep 1
adb shell cmd package compile -m speed -f app.impels 2>&1 | tail -1
adb shell cmd package compile -m speed -f app.impels.test 2>&1 | tail -1

# Runs an instrumentation test in the background and, while it runs, captures a
# screenshot via gRPC for every "<name>.req" the test drops in $SIGDIR (ack'd back).
run_test_with_handshake() {
  local method="$1" log="$2"
  adb shell pm clear app.impels 2>/dev/null || true; sleep 1
  adb shell pm grant app.impels android.permission.POST_NOTIFICATIONS 2>/dev/null || true
  adb shell mkdir -p "$SIGDIR" 2>/dev/null || true; adb shell rm -f "$SIGDIR"/* 2>/dev/null || true
  adb shell am instrument -w -e class "app.impels.$method" "$RUNNER" > "$log" 2>&1 &
  local instr=$! captured=""
  while kill -0 "$instr" 2>/dev/null; do
    for req in $(adb shell ls "$SIGDIR" 2>/dev/null | tr -d '\r' | grep '\.req$'); do
      local name="${req%.req}"
      case " $captured " in *" $name "*) continue;; esac
      sleep 0.4  # let the frame settle after waitForIdle
      "$PY" "$GRPC" snap "$OUTDIR/$name.png" 2>/dev/null
      adb shell "echo ok > $SIGDIR/$name.ack" 2>/dev/null || true
      echo "  📸 $name ($(wc -c < "$OUTDIR/$name.png" | tr -d ' ') B)"
      captured="$captured $name"
    done
    sleep 0.3
  done
  wait "$instr" 2>/dev/null || true
  grep -qE "OK \(" "$log" && echo "  ✓ $method OK" || { echo "  ⚠️ $method failed:"; tail -15 "$log"; }
}

# ── 4. Stills: 7 in-app states (CaptureFlowTest) ──────────────
say "Capturing 7 in-app states…"
run_test_with_handshake "CaptureFlowTest#captureAllStates" /tmp/instr-stills.log

# ── 5. Alarm screen (exported=false → launched via ActivityScenario) ──
say "8/8 — Alarm screen…"
run_test_with_handshake "AlarmCaptureTest#captureAlarm" /tmp/instr-alarm.log

# ── 6. Demo video: record gRPC frames while the demo test drives ──
say "Demo video — recording while demoWalkthrough drives…"
adb shell pm clear app.impels 2>/dev/null || true
adb shell pm grant app.impels android.permission.POST_NOTIFICATIONS 2>/dev/null || true
FRAMEDIR="$OUTDIR/_frames"
"$PY" "$GRPC" record "$FRAMEDIR" 45 &
REC=$!
adb shell am instrument -w -e class app.impels.CaptureFlowTest#demoWalkthrough \
  "$RUNNER" > /tmp/instr-demo.log 2>&1 || true
wait "$REC" 2>/dev/null || true

say "Encoding demo.mp4…"
IN_FPS=$(cat "$FRAMEDIR/fps.txt" 2>/dev/null || echo 3)
if ffmpeg -y -framerate "$IN_FPS" -i "$FRAMEDIR/frame-%05d.png" \
    -vf "minterpolate=fps=30:mi_mode=dup,scale=1080:2400" \
    -c:v libx264 -pix_fmt yuv420p -crf 20 "$OUTDIR/demo.mp4" 2>/dev/null; then
  rm -rf "$FRAMEDIR"
else
  ffmpeg -y -framerate "$IN_FPS" -i "$FRAMEDIR/frame-%05d.png" \
    -c:v libx264 -pix_fmt yuv420p -crf 20 "$OUTDIR/demo.mp4" 2>/dev/null && rm -rf "$FRAMEDIR"
fi

# ── 7. Done ───────────────────────────────────────────────────
say "Shutting down…"
adb emu kill 2>/dev/null || true
say "Output:"; ls -lh "$OUTDIR"/
