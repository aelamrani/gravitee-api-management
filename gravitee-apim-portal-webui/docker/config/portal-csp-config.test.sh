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

# Tests portal-csp-config.sh against the nginx configurations shipped in the image.
# Usage: sh portal-csp-config.test.sh

HERE=$(cd "$(dirname "$0")" && pwd)
SCRIPT="$HERE/portal-csp-config.sh"
FAILURES=0
STRICT="default-src 'none'; script-src 'self'; style-src 'self'; frame-ancestors 'self'; report-uri https://csp.example.com/report"

fail() {
    echo "FAIL: $1" >&2
    FAILURES=$((FAILURES + 1))
}

new_conf_dir() {
    dir=$(mktemp -d "${TMPDIR:-/tmp}/portal-csp-test.XXXXXX") || exit 1
    cp "$HERE"/default.conf "$HERE"/default.no-ipv6.conf "$HERE"/default-next.conf "$HERE"/default-next.no-ipv6.conf "$dir"/
    echo "$dir"
}

count() {
    grep -c -F "$1" "$2"
}

test_keeps_default_policy_when_nothing_is_set() {
    dir=$(new_conf_dir)
    env -u PORTAL_CSP_POLICY -u PORTAL_CSP_REPORT_ONLY_POLICY sh "$SCRIPT" "$dir" > /dev/null || fail "keeps default: script exited $?"
    for f in default.conf default.no-ipv6.conf default-next.conf default-next.no-ipv6.conf; do
        cmp -s "$HERE/$f" "$dir/$f" || fail "keeps default: $f was modified"
    done
    rm -rf "$dir"
}

test_replaces_enforced_policy() {
    dir=$(new_conf_dir)
    PORTAL_CSP_POLICY="$STRICT" sh "$SCRIPT" "$dir" > /dev/null || fail "replaces policy: script exited $?"
    for f in default.conf default.no-ipv6.conf default-next.conf default-next.no-ipv6.conf; do
        [ "$(count "add_header Content-Security-Policy \"$STRICT\" always;" "$dir/$f")" = 1 ] || fail "replaces policy: $f has no custom policy"
        [ "$(count "'unsafe-eval'" "$dir/$f")" = 0 ] || fail "replaces policy: $f still has the default policy"
        [ "$(count 'Content-Security-Policy-Report-Only' "$dir/$f")" = 0 ] || fail "replaces policy: $f has an unexpected report-only header"
        [ "$(count 'add_header X-Content-Type-Options nosniff;' "$dir/$f")" = 1 ] || fail "replaces policy: $f lost its other headers"
    done
    rm -rf "$dir"
}

test_adds_report_only_policy_and_keeps_default() {
    dir=$(new_conf_dir)
    PORTAL_CSP_REPORT_ONLY_POLICY="$STRICT" sh "$SCRIPT" "$dir" > /dev/null || fail "report-only: script exited $?"
    for f in default.conf default.no-ipv6.conf default-next.conf default-next.no-ipv6.conf; do
        [ "$(count "add_header Content-Security-Policy-Report-Only \"$STRICT\" always;" "$dir/$f")" = 1 ] || fail "report-only: $f has no report-only header"
        [ "$(count 'frame-ancestors $ALLOWED_FRAME_ANCESTOR_URLS' "$dir/$f")" = 1 ] || fail "report-only: $f lost the default enforced policy"
    done
    rm -rf "$dir"
}

test_is_idempotent_across_restarts() {
    dir=$(new_conf_dir)
    PORTAL_CSP_POLICY="$STRICT" PORTAL_CSP_REPORT_ONLY_POLICY="$STRICT" sh "$SCRIPT" "$dir" > /dev/null
    cp "$dir/default.conf" "$dir/first-run"
    PORTAL_CSP_POLICY="$STRICT" PORTAL_CSP_REPORT_ONLY_POLICY="$STRICT" sh "$SCRIPT" "$dir" > /dev/null
    cmp -s "$dir/first-run" "$dir/default.conf" || fail "idempotent: second run changed default.conf"
    rm -rf "$dir"
}

test_refuses_a_policy_with_a_double_quote() {
    dir=$(new_conf_dir)
    if PORTAL_CSP_POLICY="default-src 'self'\" always; add_header X-Injected \"1" sh "$SCRIPT" "$dir" 2> /dev/null > /dev/null; then
        fail "refuses double quote: script succeeded"
    fi
    cmp -s "$HERE/default.conf" "$dir/default.conf" || fail "refuses double quote: default.conf was modified"
    rm -rf "$dir"
}

test_keeps_default_policy_when_nothing_is_set
test_replaces_enforced_policy
test_adds_report_only_policy_and_keeps_default
test_is_idempotent_across_restarts
test_refuses_a_policy_with_a_double_quote

if [ "$FAILURES" -ne 0 ]; then
    echo "$FAILURES failure(s)" >&2
    exit 1
fi
echo "portal-csp-config.sh: all tests passed"
