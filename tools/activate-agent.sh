#!/system/bin/sh
# SPDX-License-Identifier: GPL-3.0-only
# Run on Android through adb shell, not in your computer shell.
set -eu
content call --uri content://dev.evoker.homeholdcts.desktopsearch --method stop
i=0
while pidof colorgoogle-search >/dev/null; do
  i=$((i+1)); if [ "$i" -gt 10 ]; then echo Agent_did_not_stop; exit 1; fi
  sleep 1
done
apk=$(pm path --user 0 dev.evoker.homeholdcts | sed -n "s/^package://p" | head -n 1)
test -r "$apk"
export CLASSPATH="$apk"
nohup app_process / --nice-name=colorgoogle-search dev.evoker.homeholdcts.DesktopSearchAgent </dev/null >/data/local/tmp/colorgoogle-search.log 2>&1 &
sleep 2
content call --uri content://dev.evoker.homeholdcts.desktopsearch --method status
cat /data/local/tmp/colorgoogle-search.log
