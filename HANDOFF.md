# Handoff: impels Screenshot Capture

## ✅ SOLVED — use the emulator gRPC framebuffer, not `adb screencap`

`adb screencap` / `screenrecord` read Android's HWC-composed layer, which is an
opaque black surface on this headless Intel-Mac emulator. The **emulator's own
gRPC API** (`android.emulation.control.EmulatorController.getScreenshot`, port
8554) reads the GPU framebuffer **before** HWC composition, so it returns the
real rendered frame (dark `#0D1014` bg + amber logo, ~77% non-black pixels).

**Setup (one-time, already done):**
- Boot flags: `-gpu swiftshader_indirect -grpc 8554`
- `.capture/venv` — Python venv with `grpcio grpcio-tools pillow`
- `.capture/pb/` — stubs generated from `$SDK/emulator/lib/emulator_controller.proto`
- `grpc_capture.py` — `snap OUT.png` / `record OUTDIR SECONDS` (repo root)
- Auth: Bearer token from `~/.emulator_console_auth_token` as gRPC metadata
- `.capture/` is gitignored; regenerate via the commands in that dir if missing.

`capture-screens.sh` now uses this for both stills and video. Video: gRPC PNG
grabs cap at ~2 fps on this host, so the recorder polls max-rate for the
walkthrough duration and ffmpeg encodes real-time + `minterpolate` to 24 fps.

## ✅ ALSO SOLVED — all 8 states via an instrumentation UI test

adb-driven navigation was defeated by three emulator issues (splash-tap loss,
soft-keyboard occluding Save, flaky/empty `uiautomator dump`). The fix is to
drive the app from an **`androidTest` Compose UI test** using the testTags —
in-process, so none of those apply.

**Use `run-instrumented-capture.sh`** (not the old adb-driven `capture-screens.sh`):
- `CaptureFlowTest#captureAllStates` navigates via `onNodeWithTag(...).performClick()`
  / `performTextInput(...)` and at each of the 7 in-app states writes
  `<name>.req` to the app's external files dir; the script gRPC-captures and
  writes `<name>.ack`. `performTextInput` injects text with no IME → no keyboard.
- `AlarmCaptureTest#captureAlarm` launches the `exported=false` AlarmActivity via
  `ActivityScenario` (adb `am start` is permission-denied for it).
- `CaptureFlowTest#demoWalkthrough` drives a slow ~45s pass while a gRPC recorder
  runs, for the demo video.

**Two prerequisites the script sets up (both essential on this box):**
1. **AVD RAM ≥ 4 GB** — `hw.ramSize=4096` in `~/.android/avd/fresh.avd/config.ini`
   (was 2048). At 2 GB the emulator thrashes.
2. **AOT-compile the app + test** — `cmd package compile -m speed -f app.impels`
   (and `app.impels.test`). Without it the process runs interpreted under
   instrumentation, misses the bind-application timeout → "failed to complete
   startup" ANR reported as `INSTRUMENTATION_RESULT: shortMsg=Process crashed`.

testTags live in MainActivity (`testTagsAsResourceId`) + on every driven control
(`fab_add`, `field_what`, `chip_who_*`, `chip_int_*`, `btn_save*`, `btn_done`,
`btn_settings`, `btn_dismiss`).

Note: the produced PNGs are ~50–100 KB (not >200 KB as the criterion below
guessed) — the dark theme compresses small; they are full 1080×2400 with correct
UI, verified visually.

## Problem

Need to capture screenshots (8 states) + demo video from an Android emulator running the **impels** app — all from a **headless CLI** on an **Intel MacBook** (macOS 13.7.8, 16 GB RAM). The user must **never see an emulator window**.

`adb exec-out screencap -p` captures a **near-black PNG** regardless of GPU mode. The app launches, the SurfaceFlinger layer exists and is visible, but the pixels are essentially all `(0,0,0,0)` or `(0,0,0,255)`.

**The app uses a dark theme (background ~#0D1014).** But even bright UI elements (white text, amber accent) are absent — only 0.3–0.4% of pixels have any RGB component > 10, and those are all at status-bar Y positions.

## What Was Tried (all produce black screenshots)

| GPU mode / flag | Result |
|---|---|
| `-no-window -gpu swiftshader_indirect` | "bad color buffer handle" errors in log |
| `-no-window -gpu host` | Black screenshots |
| `-qt-hide-window` (default host GPU) | Black screenshots |
| `-no-window -gpu guest` (Mesa inside guest) | 0% non-black (pure black) |
| Visible window (no hidden flags) | 0.3% non-black |

`screenrecord` also produces essentially empty files (~40 KB for 6 sec).

## Environment

- **Host**: Intel MacBook, macOS 13.7.8
- **SDK**: `/Users/mm/Library/Android/sdk` (emulator, platform-tools, cmdline-tools)
- **AVD**: `fresh` (API 36, google_apis_playstore, x86_64, 2 cores, 2 GB RAM)
- **APK**: `/Users/mm/Dev/appworkz/intervvval/impels.apk` (17.9 MB)
- **Script**: `/Users/mm/Dev/appworkz/intervvval/capture-screens.sh`
- **Output**: `/Users/mm/Dev/appworkz/intervvval/captures/`

## Key Files

- `capture-screens.sh` — the full pipeline (boot, install, 8 screenshots, screenrecord, teardown)
- `HANDOFF.md` — this file
- Config: `~/.android/avd/fresh.avd/config.ini` (currently `hw.gpu.mode=host`)
- Logs: `/tmp/emu-impels.log`

## Potential Solution Avenues

1. **Emulator gRPC screenshot API** (most promising). The emulator exposes gRPC on port 8554. It has a `getScreenshot` RPC that reads the emulator's internal framebuffer (not Android's screencap). Use `grpcurl` or a Python gRPC client with the emulator's proto (`emulator_controller.proto` from the SDK):
   ```
   $ANDROID_HOME/emulator/lib/emulator_controller.proto
   ```
   The RPC is at `android.emulation.control.EmulatorController.getScreenshot`. This might capture the actual rendered frame from the GPU before the HWC composes it.

2. **Environment variables** before boot:
   - `ANGLE_DEFAULT_PLATFORM=swiftshader` (forces ANGLE to use SwiftShader)
   - `LIBGL_ALWAYS_SOFTWARE=1` (forces Mesa/LLVMpipe software rendering)
   - `SDL_VIDEODRIVER=dummy` (SDL dummy driver, might avoid window creation)

3. **Create a virtual display** with XQuartz on macOS and start the emulator within it:
   - Install XQuartz
   - `open -a XQuartz --args -nowindow -display :1`
   - `DISPLAY=:1 emulator -avd fresh -gpu host ...`

4. **Use `scrcpy`** for frame capture (works with the emulator's ADB-based mirroring, captures the actual display content).

5. **Different AVD**: Try API 34 instead of API 36 preview.

6. **Connect via VNC**: Start emulator with `-vnc :1` and capture via VNC client.

7. **Modify `surface_flinger` properties** to force CPU composition instead of HWC:
   - `adb shell service call SurfaceFlinger 1008 i32 1` (force GPU composition)
   - `setprop debug.sf.enable_hwc_vds 0`
   - `setprop debug.sf.hw 0` (disable hardware composer)
   - `setprop ro.hwui.use_vulkan false`

8. **Use `screencap` via root/raw framebuffer**: On some emulator configurations, `/dev/graphics/fb0` exists and can be read directly. (Not available on API 36 preview by default.)

## Success Criteria

- 8 PNG screenshots (1080 × 2400) with visible UI content (not black), each >200 KB
- 1 MP4 video (45 sec walkthrough, >5 MB)
- All operations completed from CLI with no visible window

## Running the Script

```bash
export ANDROID_HOME=/Users/mm/Library/Android/sdk
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
cd /Users/mm/Dev/appworkz/intervvval
bash capture-screens.sh
```
