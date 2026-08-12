#!/bin/sh
set -eu

enabled=false
if [ "$(printf '%s' "${DEMO_MODE:-false}" | tr '[:upper:]' '[:lower:]')" = "true" ]; then
  enabled=true
fi

target=/usr/share/nginx/html/demo-config.js
temporary="${target}.tmp"

jq -n \
  --argjson enabled "$enabled" \
  --arg adminEmail "${DEMO_ADMIN_EMAIL:-}" \
  --arg adminPassword "${DEMO_ADMIN_PASSWORD:-}" \
  --arg doctorEmail "${DEMO_DOCTOR_EMAIL:-}" \
  --arg doctorPassword "${DEMO_DOCTOR_PASSWORD:-}" \
  --arg patientEmail "${DEMO_PATIENT_EMAIL:-}" \
  --arg patientPassword "${DEMO_PATIENT_PASSWORD:-}" \
  '{enabled: $enabled, adminEmail: $adminEmail, adminPassword: $adminPassword, doctorEmail: $doctorEmail, doctorPassword: $doctorPassword, patientEmail: $patientEmail, patientPassword: $patientPassword}' \
  | sed '1s/^/window.__MEDILINK_DEMO_CONFIG__ = /; $s/$/;/' > "$temporary"

mv "$temporary" "$target"
