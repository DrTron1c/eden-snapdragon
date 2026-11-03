#!/bin/bash
# Publishes the build state and the log's tail to the ci-status branch, so a build can be followed
# with plain git. Usage: report-status.sh <state> [log file]
set -uo pipefail
state=$1
log=${2:-}
dir=$(mktemp -d)
cd "$dir"
git init -q -b ci-status
git config user.name "github-actions"
git config user.email "github-actions@users.noreply.github.com"
{
    echo "state: $state"
    echo "run: $GITHUB_RUN_NUMBER"
    echo "commit: $GITHUB_SHA"
    echo "updated: $(date -u +%FT%TZ)"
} > status.txt
if [ -n "$log" ] && [ -f "$log" ]; then
    grep -nE '(error:|FAILED|Exception|What went wrong)' "$log" | tail -n 120 > errors.txt || true
    tail -n 300 "$log" > build-tail.txt
fi
git add .
git commit -q -m "build $GITHUB_RUN_NUMBER: $state"
git push -q -f "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" ci-status
