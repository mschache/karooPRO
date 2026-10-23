#!/bin/zsh
# Speaks out loud while you blind-tap the Karoo's "Allow USB debugging" dialog.
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
last=""
say "Helper ready. Unplug and replug the Karoo."
for i in $(seq 1 1800); do
  line=$("$ADB" devices -l | sed -n 2p)
  tid=$(echo "$line" | sed -n 's/.*transport_id:\([0-9]*\).*/\1/p')
  if echo "$line" | grep -q " device "; then
    say "Authorized"
    echo "AUTHORIZED: $line"
    exit 0
  fi
  if [[ -n "$tid" && "$tid" != "$last" ]]; then
    say "Karoo connected. Dialog ready."
    last="$tid"
    echo "$(date +%T) transport $tid unauthorized"
  fi
  sleep 1
done
say "Helper stopped"
