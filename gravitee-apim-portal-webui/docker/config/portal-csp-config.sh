#!/bin/sh
#
# Copyright (C) 2015-2022 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Overrides the Content-Security-Policy of the portal nginx configuration and,
# optionally, adds a Content-Security-Policy-Report-Only header:
#   PORTAL_CSP_POLICY              replaces the whole enforced policy
#   PORTAL_CSP_REPORT_ONLY_POLICY  adds a report-only policy (e.g. to trial a stricter one)
# Unset or empty variables keep the image default. Must run after portal-next-run.sh
# and check_ip_config.sh, which replace default.conf.

CONF_DIR=${1:-/etc/nginx/conf.d}
PORTAL_CSP_POLICY=${PORTAL_CSP_POLICY:-}
PORTAL_CSP_REPORT_ONLY_POLICY=${PORTAL_CSP_REPORT_ONLY_POLICY:-}

if [ -z "$PORTAL_CSP_POLICY" ] && [ -z "$PORTAL_CSP_REPORT_ONLY_POLICY" ]; then
    exit 0
fi

# Each value is written inside a double-quoted nginx string.
for value in "$PORTAL_CSP_POLICY" "$PORTAL_CSP_REPORT_ONLY_POLICY"; do
    case "$value" in
        *'"'* | *'\'*)
            echo "Invalid PORTAL_CSP_POLICY or PORTAL_CSP_REPORT_ONLY_POLICY: double quotes and backslashes are not allowed" >&2
            exit 1
            ;;
    esac
done

for conf in "$CONF_DIR"/*.conf; do
    [ -f "$conf" ] || continue
    grep -q 'add_header Content-Security-Policy "' "$conf" || continue

    tmp="$conf.csp.tmp"
    : > "$tmp"
    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            *'add_header Content-Security-Policy-Report-Only "'*)
                # Re-emitted next to the enforced policy, so a container restart does not duplicate it.
                ;;
            *'add_header Content-Security-Policy "'*)
                if [ -n "$PORTAL_CSP_POLICY" ]; then
                    printf '    add_header Content-Security-Policy "%s" always;\n' "$PORTAL_CSP_POLICY" >> "$tmp"
                else
                    printf '%s\n' "$line" >> "$tmp"
                fi
                if [ -n "$PORTAL_CSP_REPORT_ONLY_POLICY" ]; then
                    printf '    add_header Content-Security-Policy-Report-Only "%s" always;\n' "$PORTAL_CSP_REPORT_ONLY_POLICY" >> "$tmp"
                fi
                ;;
            *)
                printf '%s\n' "$line" >> "$tmp"
                ;;
        esac
    done < "$conf"
    mv "$tmp" "$conf"
done

echo "Portal Content-Security-Policy configured from PORTAL_CSP_POLICY / PORTAL_CSP_REPORT_ONLY_POLICY"
