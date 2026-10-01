#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# Taps can't be timed reliably through adb, so the sample opens each screen from the `scene` extra,
# with entry animations off and highlights (tooltips) preselected, so every capture is the same.
# Every capture is checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh          # phone: dashboard, line, bars and donut, light and dark
#   bash scripts/screenshots.sh tablet   # tablet: tablet-light.png (two column dashboard)
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# The text each scene must show; the capture fails without it.
expected_text() {
  case "$1" in
    dashboard) echo "Total balance" ;;
    line) echo "Net cash flow" ;;
    bars) echo "Active minutes" ;;
    donut) echo "Macros" ;;
    tablet) echo "Weekly steps" ;;
  esac
}

install_sample
if [ "$device" = tablet ]; then
  set_night_mode light
  fresh_launch --es scene dashboard
  capture "tablet-light" "$(expected_text tablet)"
else
  for mode in light dark; do
    set_night_mode "$mode"
    for scene in dashboard line bars donut; do
      fresh_launch --es scene "$scene"
      capture "$scene-$mode" "$(expected_text "$scene")"
    done
  done
fi
