# Reviewing and Integrating Official sdrtrunk Updates

This fork tracks the official project as a read-only Git remote named `upstream`. Official changes are reviewed before they are merged so that the fork's web console, remote APIs, audio handling, NXDN changes, and Linux packaging are not overwritten accidentally.

## One-time local setup

The upstream remote is configured in this project checkout. For any new clone of the fork, add it once and make the official destination fetch-only:

```bash
git remote add upstream https://github.com/DSheirer/sdrtrunk.git
git remote set-url --push upstream DISABLED
git config remote.pushDefault origin
git fetch upstream master --prune --no-tags
```

`origin` remains Rob Gwi's fork and is the only normal push destination. `upstream` is the official source used for comparisons.

## Automated review

The **Review Official sdrtrunk Changes** GitHub Actions workflow runs every Monday and can also be started manually from the repository's **Actions** page.

The workflow:

1. Fetches `DSheirer/sdrtrunk` without changing the fork.
2. Compares the official `master` branch with the fork's common baseline.
3. Lists new official commits, all upstream-changed files, and files changed by both projects.
4. Saves the complete Markdown report as a workflow artifact and job summary.
5. Creates or updates one GitHub issue when upstream changes need review.
6. Closes that issue after the fork contains the current official history.

The workflow never merges, rebases, pushes, builds a release, or changes a playlist.

## Run a local review

Fetch the current official history and generate the same report:

```bash
git fetch upstream master --prune --no-tags
scripts/upstream-report.sh upstream/master upstream-review.md master
```

Open `upstream-review.md` and pay particular attention to **Files changed in both projects**. Those files are not guaranteed conflicts, but they need deliberate review.

## Integrate reviewed changes

Never test an upstream merge directly on `master`. Start from a clean checkout and use a dated integration branch:

```bash
git switch master
git pull --ff-only origin master
git switch -c upstream-sync-YYYY-MM-DD
git merge --no-ff upstream/master
```

Resolve conflicts while preserving the fork-specific features. Then run:

```bash
./gradlew test
./gradlew runtimeZipLinuxX86_64
./gradlew runtimeZipRaspberryPi
```

Before merging the integration branch, manually verify:

- Desktop and headless startup.
- Web-console authentication, dashboard, live audio, recordings, and playlist editing.
- Remote Call and Rdio Scanner uploads, queue recovery, and heartbeats.
- RadioReference browsing and imports.
- Local and hosted Whisper transcription.
- NXDN repeater, known simplex, and unknown-radio simplex recording.
- Linux x86_64 and Raspberry Pi ARM64 packages.

Back up the playlist before opening it with an upstream-integration build. Configuration migrations made by official sdrtrunk may not be reversible by an older build.

After testing, push only the integration branch and review its changes before merging it into `master`:

```bash
git push -u origin upstream-sync-YYYY-MM-DD
```

Use a normal merge commit for upstream integrations. Do not rebase the fork's published `master` history or force-push it.
