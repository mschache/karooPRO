#!/bin/zsh
# Live mirror + control of a USB-connected Karoo on this Mac (scrcpy).
# Double-click in Finder, or run from a terminal. Close the window to stop.
ADB_BIN="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
SCRCPY_BIN="$(command -v scrcpy || ls -d "$HOME"/.local/scrcpy-macos-*/scrcpy 2>/dev/null | sort -V | tail -1)"
if [[ ! -x "$SCRCPY_BIN" ]]; then
  echo "scrcpy not found. Get it from https://github.com/Genymobile/scrcpy/releases"
  exit 1
fi
echo "Waiting for the Karoo on USB (plug it in)…"
"$ADB_BIN" wait-for-usb-device
state=$("$ADB_BIN" -d get-state 2>&1)
if [[ "$state" != "device" ]]; then
  echo "Karoo connected but not authorized ($state): accept 'Allow USB debugging' on the device."
  exit 1
fi
# Use the SDK's adb so scrcpy and other tools share one adb server.
export ADB="$ADB_BIN"
exec "$SCRCPY_BIN" -d --window-title "Karoo (live)" --no-audio --stay-awake --max-fps 30 "$@"
