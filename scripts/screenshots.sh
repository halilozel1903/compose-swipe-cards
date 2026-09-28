#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# Swipes can't be performed reliably by adb, so the sample sets up each scene from the `scene` extra.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
for mode in light dark; do
  set_night_mode "$mode"
  for scene in deck dragging liked; do
    fresh_launch --es scene "$scene"
    capture "$scene-$mode"
  done
done
