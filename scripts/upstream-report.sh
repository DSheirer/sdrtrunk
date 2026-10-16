#!/usr/bin/env bash

set -euo pipefail

UPSTREAM_REF="${1:-upstream/master}"
OUTPUT_FILE="${2:-upstream-review.md}"
FORK_REF="${3:-HEAD}"

if ! git rev-parse --verify --quiet "${UPSTREAM_REF}^{commit}" >/dev/null; then
    echo "Unable to find ${UPSTREAM_REF}. Run 'git fetch upstream master --prune --no-tags' first." >&2
    exit 1
fi

if ! git rev-parse --verify --quiet "${FORK_REF}^{commit}" >/dev/null; then
    echo "Unable to find fork reference ${FORK_REF}." >&2
    exit 1
fi

MERGE_BASE="$(git merge-base "${FORK_REF}" "${UPSTREAM_REF}")"
FORK_SHA="$(git rev-parse "${FORK_REF}^{commit}")"
UPSTREAM_SHA="$(git rev-parse "${UPSTREAM_REF}^{commit}")"
FORK_COUNT="$(git rev-list --count "${MERGE_BASE}..${FORK_REF}")"
UPSTREAM_COUNT="$(git rev-list --count "${MERGE_BASE}..${UPSTREAM_REF}")"
GENERATED_AT="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

git diff --name-only "${MERGE_BASE}..${FORK_REF}" | sort -u >"${TMP_DIR}/fork-files"
git diff --name-only "${MERGE_BASE}..${UPSTREAM_REF}" | sort -u >"${TMP_DIR}/upstream-files"
comm -12 "${TMP_DIR}/fork-files" "${TMP_DIR}/upstream-files" >"${TMP_DIR}/overlap-files"

{
    echo "# Official sdrtrunk Upstream Review"
    echo
    echo "Generated: ${GENERATED_AT}"
    echo
    echo "| Item | Value |"
    echo "| --- | --- |"
    echo "| Fork reference | \`${FORK_REF}\` at \`${FORK_SHA:0:12}\` |"
    echo "| Official reference | \`${UPSTREAM_REF}\` at \`${UPSTREAM_SHA:0:12}\` |"
    echo "| Common baseline | \`${MERGE_BASE:0:12}\` |"
    echo "| Fork-only commits | ${FORK_COUNT} |"
    echo "| New official commits | ${UPSTREAM_COUNT} |"
    echo

    if [[ "${UPSTREAM_COUNT}" -eq 0 ]]; then
        echo "The fork already contains the current official \`${UPSTREAM_REF}\` history. No integration is needed."
    else
        echo "## New official commits"
        echo
        git log --date=short --pretty='- `%h` %ad — %s' "${MERGE_BASE}..${UPSTREAM_REF}"
        echo
        echo "## Files changed in both projects"
        echo
        if [[ -s "${TMP_DIR}/overlap-files" ]]; then
            while IFS= read -r FILE; do
                echo "- \`${FILE}\`"
            done <"${TMP_DIR}/overlap-files"
        else
            echo "No files were changed on both sides of the common baseline. A test integration is still required."
        fi
        echo
        echo "## All files changed by the official project"
        echo
        while IFS= read -r FILE; do
            echo "- \`${FILE}\`"
        done <"${TMP_DIR}/upstream-files"
        echo
        echo "## Safe integration procedure"
        echo
        echo "1. Create a branch from the current fork: \`git switch -c upstream-sync-YYYY-MM-DD master\`."
        echo "2. Merge without publishing immediately: \`git merge --no-ff ${UPSTREAM_REF}\`."
        echo "3. Resolve each conflict while preserving the web console, remote APIs, audio queueing, NXDN recording, and multi-architecture packaging."
        echo "4. Run \`./gradlew test\`, \`./gradlew runtimeZipLinuxX86_64\`, and \`./gradlew runtimeZipRaspberryPi\`."
        echo "5. Manually test desktop and headless startup, the web console, live audio, streaming/heartbeats, RadioReference, transcription, and NXDN recording."
        echo "6. Push the integration branch to the fork and review it before merging to \`master\`."
    fi
} >"${OUTPUT_FILE}"

echo "Wrote ${OUTPUT_FILE}: ${UPSTREAM_COUNT} new official commit(s), ${FORK_COUNT} fork-only commit(s)."
