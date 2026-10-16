# sdrtrunk Web and Raspberry Pi Fork

This repository is Rob Gwi's experimental fork of [DSheirer's sdrtrunk](https://github.com/DSheirer/sdrtrunk). It adds Raspberry Pi ARM64 packaging, an embedded web console, remote call delivery, browser audio, and additional Playlist Editor workflows while retaining the Java desktop application.

> This is a beta fork and is not an official upstream sdrtrunk release. Back up your playlist before testing it.

## Current release: 0.7.1-beta4

- [Latest downloadable release: Raspberry Pi ARM64 or Linux x86_64 0.7.1 beta 4](https://github.com/robgwi/sdrtrunk/releases/tag/sdrtrunk-v0.7.1-beta4)
- [Raspberry Pi installation guide](RASPBERRY_PI.md)
- [Linux x86_64 installation guide](LINUX_X86_64.md)
- [Local Whisper setup guide](WHISPER_SETUP.md)
- [VCE web interface integration and feature status](VCE_WEB_INTERFACE.md)
- [Remote Call API and heartbeat guide](REMOTE_API.md)
- [Rdio Scanner heartbeat integration guide](RDIO_SCANNER_HEARTBEAT.md)
- [Official project update and integration guide](UPSTREAM_SYNC.md)
- [rtl_tcp network tuner guide](NETWORK_TUNER.md)
- [Upstream sdrtrunk wiki](https://github.com/DSheirer/sdrtrunk/wiki)

The Raspberry Pi archive contains its own ARM64 Java and JavaFX runtime. A separate Java installation is not needed. It supports the Java desktop plus web console when launched from Raspberry Pi Desktop or VNC, and headless receiver plus web console when launched without a display or with `-Djava.awt.headless=true`.

## Fork features

### Scanner-style web console

- Runs alongside both the desktop and headless application.
- Uses the responsive interface from [tylerwatt12/sdrtrunk-vce](https://github.com/tylerwatt12/sdrtrunk-vce), adapted to this fork's embedded Java server and existing playlist model.
- Keeps the main page focused on the live police-scanner display; management tools use the VCE top navigation and responsive workspaces.
- Shows scanning/receiving state, talkgroup ID and configured alias/name, source radio, protocol, frequency, and recent decoded activity.
- Clears the previous talkgroup, alias, frequency, source, signal, and level as soon as the receiver returns to scanning.
- Starts and stops configured channels remotely.
- Queues completed live calls in order and plays every call to completion before applying the configurable between-call hold time.
- Shows the current call metadata, measured audio level, and number of waiting calls in the top scanner panel.
- Lists and plays MP3/WAV recordings from the configured recording directory.
- Shows actual host CPU and memory usage, tuner/channel status, and streaming-destination state.
- Provides a selectable live tuner spectrum and waterfall without interrupting active channel decoders.
- Uses a password login and private browser session cookie; API clients can continue using `Authorization: Bearer <web-password>`.
- Enables, disables, and restarts configured streaming destinations from the Streaming window.

Version 0.7.1 Beta 1 replaces the previous inline browser dashboard instead of keeping a theme selector. Supported VCE pages are
backed by this fork's live APIs for the scanner dashboard, browser call queue, transcripts, channels and talkgroups,
RadioReference import, streaming and Remote Calls, tuners, spectrum/waterfall, recordings, Whisper, host metrics, and
web-password administration. VCE features that require its separate SQLite history/account subsystem—multi-user
roles, scan lists, historical analytics, managed recording catalog, and P25 network visualization—remain hidden until
equivalent backend support is implemented. See [VCE_WEB_INTERFACE.md](VCE_WEB_INTERFACE.md) for the endpoint map.

Signal strength is displayed as unavailable when a tuner does not expose calibrated RSSI through sdrtrunk's shared tuner interface. The dashboard does not fabricate a signal value.

### Web playlist management

The web channel and talkgroup editor can:

- Create and delete channels.
- Edit channel name, system, site, alias-list assignment, frequency, protocol, and auto-start.
- Start and stop channels.
- Add, edit, and delete talkgroup aliases in each channel's assigned alias list.
- Configure and test a RadioReference Premium account using the same shared service as the desktop editor.
- Browse country, state, county, statewide systems, and county systems; then preview and selectively import trunked-system talkgroups without duplicates.
- Filter RadioReference talkgroups by category or search text, create an alias list, identify previously imported entries, and optionally mark fully encrypted imports as Do Not Monitor.
- Edit talkgroup name, category, protocol, numeric ID, recording flag, playback priority, and CTCSS alias identifier.
- Select the Remote Calls destinations that receive each talkgroup, using the same alias routing model as the desktop app.
- Add, edit, enable, disable, and delete Remote Call API destinations, including authentication, heartbeat, retry, timeout, hosted Whisper, local Whisper, and translation settings.
- Configure NBFM bandwidth, assigned talkgroup, carrier/CTCSS/DCS squelch, CTCSS tone search, de-emphasis, high-pass filtering, and automatic level control.
- Include the configured talkgroup alias in saved audio filenames for easier browsing and identification.

Other advanced protocol-specific decoder fields, site/channel creation, conventional agency-frequency imports, and alias identifier types other than the web editor's talkgroup and CTCSS fields still use the Java desktop Playlist Editor. The web RadioReference workflow now mirrors the desktop trunked-talkgroup import path.

### Desktop Playlist Editor improvements

- Change an existing channel protocol without deleting and recreating the channel.
- Choose a protocol while cloning a channel.
- Preserve general channel settings while resetting incompatible protocol-specific decoder options.
- Enable **Record Unknown Simplex Radio IDs** on an NXDN channel to record inbound direct calls when either radio ID has no alias.
- Select an explicit **Conventional** or **Trunked** NXDN channel type in both the desktop and web channel editors;
  existing playlists without the field remain trunked for compatibility.
- Use **NBFM Squelch: Carrier / CTCSS / DCS** to select carrier squelch, a fixed CTCSS tone, a fixed DCS code, or CTCSS tone search.
- Add CTCSS tones as first-class alias identifiers from the NBFM alias menu; tones survive playlist saves and alias cloning and resolve against decoded CTCSS identifiers.
- Use **DCS Monitor / Code Search** from Additional Decoders when searching for an unknown DCS code.
- Manage the web console password and restart the embedded web server from **Playlist Editor > Web Interface**.
- View host CPU, host memory, and sdrtrunk JVM memory in the desktop **System** tab.

### rtl_tcp network tuners

- Add an RTL-SDR connected to another Raspberry Pi or computer as an `rtl_tcp` network tuner from the desktop
  **Tuners** tab or the web console **System** window.
- Configure the remote host, full 1-65535 port range, center frequency, sample rate, automatic/manual gain, Bias-T,
  and continuous automatic reconnect.
- Shows Connecting, Connected, Reconnecting, and error details in both interfaces and provides a manual reconnect.
- Re-sends the complete tuner configuration after every reconnect and remembers whether the tuner was enabled or disabled.
- Uses the same multi-channel channelizer and web spectrum/waterfall as a directly attached RTL-SDR.

This support is adapted from upstream pull request [#2471](https://github.com/DSheirer/sdrtrunk/pull/2471), which is
still under upstream review, so treat network tuners as experimental and keep the `rtl_tcp` connection on a trusted
network. See [NETWORK_TUNER.md](NETWORK_TUNER.md) for setup and security notes.

### Remote call API and transcription

- POST completed calls and metadata to a configurable API URL with an MP3 attachment.
- Authenticate using an API key stored in an environment variable.
- Optionally POST an authenticated JSON online heartbeat at a configurable interval and show its last connection result.
- Transcribe locally or use OpenAI `whisper-1`.
- Optionally translate supported audio to English through the hosted OpenAI workflow.
- Existing Rdio Scanner uploads continue to use `<Rdio Scanner URL>/api/call-upload`.
- Rdio Scanner destinations can optionally POST authenticated JSON heartbeats to a derived `/api/heartbeat` endpoint
  or a separately configured URL.
- Rdio Scanner reconnects indefinitely with bounded exponential backoff and keeps every unconfirmed completed call in
  a disk-backed queue across outages, edits, disable/enable cycles, and application restarts.

### Background scanner transcription

- Runs the [robgwi Whisper fork](https://github.com/robgwi/whisper) as a separate background process, so SDR decoding remains in Java and does not wait for transcription.
- Configures the executable, model, language, transcribe/translate task, timeout, scanner vocabulary prompt, numeric cleanup, and optional PII redaction from the web console.
- Recognizes scanner phonetic-alphabet runs, compacts spoken digits and license plates, and shows completed transcripts with their talkgroup and alias.
- Shows the matching call's pending/completed transcription directly below the live scanner display.
- Pins locations found from transcript text on an OpenStreetMap preview using Nominatim geocoding.

Whisper, PyTorch, ffmpeg, and model weights are intentionally not bundled in the Java archive. Follow the [local Whisper setup guide](WHISPER_SETUP.md). On a Raspberry Pi, start with `tiny.en` or `base.en`; larger models may be too slow or memory-intensive.

## Raspberry Pi quick start

This build requires a Raspberry Pi 4 or 5 running a 64-bit operating system. Confirm that `uname -m` prints `aarch64`.

```bash
sudo apt update
sudo apt install libusb-1.0-0 unzip

unzip sdr-trunk-linux-aarch64-v0.7.1-beta4.zip
cd sdr-trunk-linux-aarch64-v0.7.1-beta4
bin/sdr-trunk
```

Open `http://<raspberry-pi-address>:10000/` from another computer. A desktop/VNC first launch asks you to create a web console password. A first headless launch generates a password, saves it, and writes it once to the startup log.

To supply secrets through the launch environment instead:

```bash
export SDRTRUNK_WEB_PASSWORD='replace-with-a-long-random-password'
export SDRTRUNK_REMOTE_API_KEY='remote-destination-key'
export OPENAI_API_KEY='OpenAI-key'
bin/sdr-trunk
```

Environment variables override saved GUI password settings. The older `SDRTRUNK_WEB_TOKEN` name remains a compatibility fallback. Do not commit real passwords, keys, or tokens to this repository.

## Release history

### 0.7.1-beta4

- Fixed [upstream issue #2496](https://github.com/DSheirer/sdrtrunk/issues/2496): aliases without an explicitly
  selected color now inherit the active theme's table text color in Now Playing and the Events Log. An explicitly
  selected black color remains black.
- Completed [upstream issue #2498](https://github.com/DSheirer/sdrtrunk/issues/2498): each Broadcastify Calls
  destination can now send either the received over-the-air talker alias or the stored playlist radio alias in the
  `srcId_alias` field.
- Added opt-in OTA alias storage for [upstream issue #2497](https://github.com/DSheirer/sdrtrunk/issues/2497).
  Completed calls sent to that Broadcastify destination create or update an exact radio alias in the channel's active
  playlist alias list. Radio-ID ranges are never renamed by this feature.
- Added the Broadcastify talker-alias source and playlist-storage settings to both the Java Playlist Editor and VCE
  web Streaming editor, with clone/persistence coverage and focused regression tests.

### 0.7.1-beta3

- Fixed [upstream issue #2499](https://github.com/DSheirer/sdrtrunk/issues/2499), where Phase 2 traffic channels
  created by a Phase 1 control channel silently reverted to the default JMBE audio configuration.
- Phase 1 and Phase 2 traffic channels now inherit the parent channel's JMBE automatic gain control, tone-generator
  gain, and comfort-noise gain even when the traffic channel uses the opposite P25 phase.
- Added regression tests for both Phase 1-to-Phase 2 and Phase 2-to-Phase 1 traffic-channel configuration inheritance.

### 0.7.1-beta2

- Fixed [upstream issue #2475](https://github.com/DSheirer/sdrtrunk/issues/2475), where NXDN trunked call events
  disappeared when a channel-mode system had no channel-to-frequency map entry. The Events tab now tracks unresolved
  grants by their logical channel number without attempting to allocate a tuner.
- Kept unresolved logical channels isolated so simultaneous calls on different unmapped channels are not merged.
- Retained completed NXDN call trackers through the existing duplicate-suppression window so residual control-channel
  grants no longer create multiple sub-second event rows or immediately reallocate a traffic channel.
- Added regression tests for unmapped-channel event tracking, logical-channel isolation, residual-grant suppression,
  and creation of a new event after the suppression window expires.

### 0.7.1-beta1

- Added explicit Conventional/Trunked NXDN channel modes, conventional call start/progress/release/disconnect handling,
  conventional multi-frequency rotation behavior, clone persistence, RadioReference trunked defaults, and matching web
  channel controls, adapted from `tylerwatt12/sdrtrunk-vce`.
- Accepted NXDN null-group ID 0 for conventional/private-call routing and allowed an encrypted channel state to recover
  directly to clear call, active, or data states.
- Fixed a synchronous NXDN traffic-channel start/rejection allocation race, increased the NXDN traffic fade timeout to
  eight seconds, and bounded pre-header audio caching so a missing call header cannot grow memory indefinitely.
- Added NXDN source-heartbeat health monitoring with last-valid-message/audio diagnostics and controlled standard-channel
  restart attempts using bounded exponential backoff. Legitimately idle scanning does not trigger recovery.
- Added regression coverage for legacy NXDN configuration compatibility, conventional call cleanup, channel cloning and
  rotation states, null-group validity, encrypted-state recovery, and the bounded audio cache.
- Replaced the fork's original inline web dashboard with the responsive VCE web interface from
  [tylerwatt12/sdrtrunk-vce](https://github.com/tylerwatt12/sdrtrunk-vce), retaining its GPL notice and author credit.
- Added VCE-compatible authentication, administrator session, saved browser preferences, static-resource delivery,
  revision reload detection, password change, and web-server restart contracts to the embedded Java server.
- Added VCE-styled adapters for the existing live scanner, sequential call playback, transcripts, channel/talkgroup
  editing, NBFM/CTCSS/DCS controls, NXDN unknown-simplex recording, RadioReference import, streaming health,
  Remote Calls and heartbeats, tuner management, spectrum/waterfall, recordings, Whisper, and host metrics.
- Removed the old embedded dashboard document and made the VCE interface the only browser console.
- Kept VCE database-only features hidden rather than exposing controls without working backend support; documented the
  current feature and endpoint coverage in `VCE_WEB_INTERFACE.md`.
- Made local Whisper remain quiet and inactive when it is disabled or its executable is unavailable.
- Added preflight validation for absolute paths, home-directory paths, and executable names found on `PATH` before a
  recorded call is submitted for transcription.
- Replaced repeated missing-executable stack traces with one concise warning per distinct configuration or runtime
  problem.
- Added live Whisper availability, resolved-path, configuration-error, and last-runtime-error status to the web
  console dashboard and Settings window.
- Clarified that the Whisper executable field accepts only a command name or executable path, not a terminal command.
- Completed the VCE Streaming workspace with create/edit/delete support for Rdio Scanner, Broadcastify Calls,
  OpenMHz, Broadcastify Feed, Icecast, and Shoutcast destinations, including URLs, credentials, system IDs,
  heartbeat/test options, queue limits, audio metadata, connection health, reconnect, and clone controls.
- Expanded web talkgroup routing so aliases can target native streaming destinations as well as Remote Call APIs;
  destination renames preserve alias routing and destination deletion removes stale assignments.
- Added per-channel call-event and decoded-message logging controls to the web Playlist editor.
- Added authenticated Application Log and Event Log viewers, including severity/search controls, recent-log tailing,
  event-log selection, and complete event-log downloads.
- Expanded the VCE Tuners workspace with complete rtl_tcp creation and editing for host, port, center frequency,
  sample rate, automatic/manual gain, Bias-T, and persistent auto-reconnect settings.
- Added web editing for attached RTL2832/RTL-SDR center frequency, sample rate, PPM correction, Bias-T, and R8x master
  gain, while matching the desktop GUI's frequency/sample-rate lock whenever decoder channels are assigned.
- Added tuner allocation, usable-bandwidth, configuration-lock, active-channel, network-health, and diagnostic logging
  details to the web console, with safeguards that prevent disruptive retuning while decoder channels are active.
- Upgraded the browser spectrum and waterfall with selectable FFT size/window, zoom, pan, dB range, smoothing,
  waterfall palettes, update-rate controls, frequency and dB axes, active-channel bandwidth overlays, cursor readout,
  and confirmed click-to-tune for idle tuners.

### 0.7.0-beta-24

- Expanded the API-key controls in the Rdio Scanner, Broadcastify Calls, and OpenMHz desktop connection editors so
  long keys remain visible while editing.
- Expanded the Remote Calls web-console API-key control to the full width of its connection editor.
- Verified with authenticated heartbeat tests that 16-character API keys are stored and transmitted without
  truncation; sdrtrunk does not impose a fixed API-key length.
- Standardized the Raspberry Pi release filename as `sdr-trunk-linux-aarch64-v<version>.zip`.

### 0.7.0-beta-23

- Converged the fork with seven official sdrtrunk updates through upstream commit `0460f2b7`, retaining the web
  console, dual-architecture packaging, NXDN unknown-simplex recording, Remote Calls, Rdio Scanner recovery, and
  transcription changes.
- Updated to Java 27, Gradle 9.8.1, and JMBE API 2.0; added per-protocol JMBE automatic gain control, tone gain, and
  noise gain controls for P25, DMR, and NXDN plus improved JMBE creator diagnostics.
- Integrated the official P25 Phase 1 excessive-audio-gain correction and theme-aware main-window/Playlist Editor
  icons, including the Material Deep Ocean theme fix.
- Adapted experimental `rtl_tcp` network-tuner support from upstream pull request #2471 with desktop add/remove and
  tuning controls, saved host/port/sample-rate/gain/Bias-T settings, and normal sdrtrunk channel/spectrum support.
- Hardened network tuners with full-range ports, handshake timeouts, socket keepalive, immediate clean shutdown,
  indefinite bounded reconnects by default, complete configuration replay, and visible connection/retry errors.
- Added matching web-console network-tuner status, add, enable/disable, reconnect, delete, and spectrum selection.

### 0.7.0-beta-22

- Changed Rdio Scanner connection recovery to retry forever with exponential backoff capped at 60 seconds; a failed
  attempt remains a temporary retrying state instead of becoming a terminal error.
- Added a disk-backed Rdio Scanner call queue under the configured streaming directory. Unconfirmed calls now survive
  endpoint outages, destination edits, disable/enable cycles, and complete sdrtrunk restarts.
- Removed Rdio Scanner queue age-off during outages so a call remains queued until Rdio Scanner accepts it or rejects
  it as a duplicate.
- Kept pending queue counts visible in both the desktop Streaming editor and web console while a destination is disabled.
- Guarded the queue worker against unexpected exceptions so one bad transaction cannot permanently stop reconnecting.
- Separated heartbeat health from call-upload connection state, preventing a separate heartbeat endpoint from falsely
  marking the Rdio call-upload service connected or disconnected.

### 0.7.0-beta-21

- Added a **Clone** action to the desktop Streaming editor and web Streaming window for duplicating any configured streaming destination.
- Clones preserve destination-specific settings and alias/talkgroup routing, including Rdio Scanner credentials and heartbeat options, while starting disabled to prevent accidental duplicate uploads.
- Expanded the desktop Rdio Scanner URL field to show the complete `/api/call-upload` endpoint.
- Added a wide, wrapping URL column to the web Streaming window and widened the Remote Call API URL editor.

### 0.7.0-beta-20

- Fixed AM and NBFM channel-name metadata so the configured Playlist channel name appears in the desktop Now Playing channel display.
- Added the configured channel name to the web console's Live Traffic Scanner display and live-call metadata.
- Made Rdio Scanner use the configured Playlist channel name as the talkgroup label fallback when no usable talkgroup alias name is available, preventing avoidable `Unknown` labels.

### 0.7.0-beta-19

- Fixed the beta 18 packaged-runtime startup failure that could leave SDR and decoder worker threads running while the Java desktop window never appeared.
- Added the `jdk.management` module to the Raspberry Pi ARM64 and Linux x86_64 runtimes so the new desktop System tab can read host CPU and memory information.
- Made host-metric discovery optional and failure-safe, ensuring unavailable platform metrics can never prevent either the desktop GUI or web console from starting.

### 0.7.0-beta-18

- Replaced the browser access-token field with a web console password login backed by expiring, HTTP-only session cookies; existing bearer clients remain compatible by using the password as the bearer credential.
- Added a dedicated Streaming window that shows connection state, queue, sent/error counts, and enable, disable, and restart controls for every configured destination.
- Added a selectable web spectrum and waterfall fed from the same tuner sample stream as the desktop display, without reserving the tuner or blocking channel decoding.
- Changed web CPU and memory reporting to actual host utilization instead of load-average estimates and JVM heap figures.
- Added a desktop **System** tab with host CPU, host RAM, logical processor count, JVM heap usage, operating system, and JVM uptime.
- Migrates the prior saved web token automatically and retains `SDRTRUNK_WEB_TOKEN` as a legacy environment-variable fallback.

### 0.7.0-beta-17

- Renamed the NBFM `None` squelch choice to **Carrier Squelch** and the desktop/web selector to **NBFM Squelch: Carrier / CTCSS / DCS**.
- Added CTCSS as a first-class alias identifier with playlist XML persistence, decoded-tone alias lookup, independent clone/copy support, desktop editing, and web-console editing.
- Added **CTCSS Tone Search**, which identifies an unknown standard CTCSS tone while carrier/noise squelch continues to control the audio.
- Renamed the auxiliary DCS decoder to **DCS Monitor / Code Search** so its search purpose is clear.
- Added matching carrier, fixed CTCSS, CTCSS search, fixed DCS, and CTCSS alias controls to the web Playlist windows.
- Fixed CTCSS and DCS alias-map cleanup when an alias is removed or edited, preventing stale tone matches.
- Added regression coverage for CTCSS lookup/removal, alias cloning, playlist persistence, tone search, and web configuration.

### 0.7.0-beta-16

- Integrated six reviewed official sdrtrunk updates without removing the fork's web console, Remote API recovery, Rdio Scanner heartbeat, transcription, packaging, or NXDN simplex-recording changes.
- Added NBFM channel-level CTCSS/DCS squelch filtering, selectable de-emphasis, high-pass audio filtering, automatic level control, and tone activity reporting.
- Added matching NBFM controls to the web Playlist Channel editor, including bandwidth, assigned talkgroup, tone type/code, de-emphasis, high-pass filtering, and automatic level control.
- Preserves all new NBFM settings when a channel is cloned, including an independent copy of its squelch configuration.
- Added Broadcastify duplicate-rejection counts to the desktop streaming table and web System window.
- Fixed P25 Phase 1 invalid frequency-band contamination, P25 Phase 2 I-ISCH fallback decoding, MDC1200 emergency summaries, and Icecast 2.5 HTTP PUT connections.
- Added focused regression coverage for NBFM cloning, web configuration, invalid tone rejection, NXDN unknown-radio recording, and Remote API queue recovery.

### 0.7.0-beta-15

- Added the official `DSheirer/sdrtrunk` repository as the documented read-only `upstream` source for this fork.
- Added a weekly and manually runnable GitHub Actions review that detects new official commits without merging them automatically.
- Generates a downloadable comparison report containing the common baseline, commit counts, official commits, upstream-changed files, and files modified by both projects.
- Creates or updates one GitHub review issue while official changes are pending and closes it after they are integrated.
- Added a safe integration guide covering isolated branches, regression tests, dual-architecture builds, manual feature checks, and playlist backups.

### 0.7.0-beta-14

- Added a per-channel **Record Unknown Simplex Radio IDs** option for NXDN conventional channels in both the desktop Playlist Editor and web channel editor.
- Records an inbound NXDN simplex/direct call when either its source or destination radio ID has no matching alias.
- Leaves known-radio recording under the existing alias **Record Audio** setting and does not apply the fallback to repeater/outbound calls.
- Preserves the new setting, along with the existing NXDN ignore-call and talker-alias encoding settings, when cloning a channel.
- Added regression coverage for unknown, partially known, fully known, and repeater/outbound radio pairs.

### 0.7.0-beta-13

- Fixed NXDN conventional simplex/direct radio-to-radio calls not triggering alias-based audio recording.
- Processes inbound NXDN voice-call headers the same as repeater/outbound headers so source radio, destination radio or talkgroup, and encryption identifiers reach the audio recorder.
- Added inbound Type-D voice-call handling for the same identifier and recording path.
- Fixed inbound NXDN transmission-release messages being classified as data instead of cleanly ending the active call.

### 0.7.0-beta-12

- Added **Send Heartbeat**, interval, and optional heartbeat URL settings to the desktop **Playlist Editor > Streaming > Rdio Scanner** editor.
- Derives `<Rdio Scanner URL>/api/heartbeat` when a separate heartbeat URL is not configured.
- Sends an authenticated JSON heartbeat with the Rdio system ID, destination, host, version, uptime, connection state, and queued-call count.
- Added exponential heartbeat retry and HTTP-client replacement after network errors, timeouts, or non-2xx responses.
- Fixed failed Rdio Scanner call uploads being removed from the queue without retrying, and added a request watchdog so a stalled upload can recover.
- Added an integration guide and Node/Express receiver example documenting the complete heartbeat contract.
- Added automated tests for authentication headers, heartbeat JSON, URL derivation, and configuration copying.

### 0.7.0-beta-11

- Fixed Remote Call API destinations that could remain stuck in **Connecting** with upload worker slots permanently occupied.
- Added a completed-call upload watchdog that cancels stalled speech/HTTP work, rebuilds the connection, and returns the call to the queue.
- Made automatic and manual reconnect retry active calls immediately instead of waiting on a stale request or its previous backoff timer.
- Preserves queued and in-flight calls when a Remote API destination is edited, disabled, re-enabled, or internally restarted; only deleting the destination releases its retained queue.
- Added **Keep retrying queued calls**, enabled by default, so a temporary outage no longer discards calls after the former retry or maximum-age limits. Backoff is capped at five minutes.
- Expanded Remote Calls status with queued and actively sending counts.
- Added automated coverage for queue preservation across a destination restart and the continuous-retry configuration.

### 0.7.0-beta-10

- Added a Remote API heartbeat watchdog that detects failed or stale connections and retries with bounded exponential backoff.
- Rebuilds the Java HTTP client after a failed heartbeat or completed-call upload so stale pooled connections cannot permanently stop delivery.
- Fixed a completed-call preparation failure that could consume an upload worker slot and eventually stall the destination.
- Preserves queued calls while reconnecting and continues the existing per-call retry policy.
- Expanded the Remote Calls web window with last contact, last heartbeat, last completed-call delivery, reconnect count, API-key availability, and detailed HTTP errors.
- Added a manual **Reconnect** action that immediately discards the current HTTP connection and runs a new heartbeat health check.
- Added an optional masked API-key field to the web destination editor while continuing to prefer the configured environment variable.
- Added automated failure/recovery coverage proving that a rejected heartbeat reconnects and returns to Connected.

### 0.7.0-beta-9

- Added an optional heartbeat toggle and configurable interval to each Remote Call API destination.
- Sends an authenticated JSON online heartbeat immediately at startup and periodically to the destination's existing POST URL.
- Uses heartbeat responses to report Connecting, Connected, and Temporary Broadcast Error states.
- Shows the last successful heartbeat time or current heartbeat error in the Remote Calls web window.
- Preserved heartbeat settings in the playlist and when copying Remote Call configurations.
- Added a Remote Call API guide with the heartbeat and completed-call request contracts.

### 0.7.0-beta-8

- Changed the embedded web-console default port from 8080 to 10000.
- Applied port 10000 consistently to the Raspberry Pi ARM64 and Linux x86_64 packages.
- Updated the desktop Playlist Editor web-address help and all Linux, Raspberry Pi, and Whisper setup instructions.
- Retained the `-Dsdrtrunk.web.port=<port>` Java property for custom port overrides.

### 0.7.0-beta-7

- Added a dedicated self-contained Linux x86_64 package for Intel and AMD desktops, mini PCs, and headless servers.
- Enabled the embedded web console on all network interfaces by default in both Linux ARM64 and x86_64 packages.
- Added Linux x86_64 installation, desktop/VNC, headless, and Whisper guidance.
- Published ARM64 and x86_64 packages together so both architectures run the same source release.

### 0.7.0-beta-6

- Added a live-call transcription panel directly below the main scanner display.
- Matched dashboard transcripts to the live audio sequence so one call's text is not shown for a different call.
- Shows pending, ready, disabled, and authentication/error states in the dashboard transcription panel.
- Added a complete Raspberry Pi local Whisper installation, configuration, service-account, model-selection, privacy, and troubleshooting guide.
- Linked the guide from the repository README and the web Whisper Settings window.

### 0.7.0-beta-5

- Fixed unidentified or undefined talkgroups being encoded and played by the web console even though the desktop recorder rejected them.
- Limited web live audio to completed, unencrypted, non-muted calls whose destination talkgroup has a configured, record-enabled alias.
- Applied the desktop duplicate-call recording preference to web live audio.
- Limited the real-time web scanner activity display to the same configured, record-enabled talkgroups so rejected traffic no longer appears as active.

### 0.7.0-beta-4

- Rebuilt the web RadioReference modal around the desktop editor's trunked-talkgroup workflow.
- Fixed sign-in failures being reported as HTTP 400; invalid, expired, unavailable, and Premium results now appear as readable account status.
- Applied successful credentials to the application's shared RadioReference service and restored saved credentials when the web service initializes.
- Added Country, State, County, and Trunked System browsing with preferred-location restoration.
- Added category/search filtering, individual and visible-row selection, alias-list creation, existing-alias detection, and selective imports.
- Added optional Do Not Monitor priority for fully encrypted imported talkgroups.
- Improved upstream/network error status while reserving HTTP 400 for malformed user input.

### 0.7.0-beta-3

- Simplified the main web page to a dedicated police-scanner dashboard.
- Moved System, Playlist, RadioReference, Recordings, Transcripts, Remote Calls, and Settings into responsive modal windows opened from the top menu.
- Added a dedicated System modal for CPU, memory, tuners, active channels, streaming destinations, and recent activity.
- Removed the completed-audio fallback from the real-time display so SCANNING never shows the last call's talkgroup, alias, frequency, source, signal, or audio level.
- Kept queued MP3 playback status separate from the real-time receiver state.

### 0.7.0-beta-2

- Fixed Live Listening replacing an in-progress call when the next completed audio file arrived.
- Added an ordered server-side buffer of up to 200 completed calls so bursts are not lost between browser polls.
- Made the browser wait for each call to finish before fetching and playing the next queued call.
- Added a browser-persisted hold-time setting, defaulting to 0.7 seconds between calls, plus a queued-call count.
- Kept displayed talkgroup, alias, source, frequency, and signal metadata synchronized with the call actually playing.
- Made manual Recorded Audio playback stop Live Listening explicitly instead of silently replacing its audio.

### 0.7.0-beta-1

- Redesigned the web console around a persistent top menu for dashboard, playlist, RadioReference, transcripts, Remote Calls, and settings.
- Added web-based RadioReference Premium credential testing plus trunked-system talkgroup preview/import into an alias list, with protocol-aware values and duplicate protection.
- Added queued background transcription through the `robgwi/whisper` command-line application.
- Added web settings for Whisper model, language, task, executable, timeout, scanner vocabulary, number cleanup, PII redaction, and map region.
- Added scanner phonetic-alphabet and numeric post-processing, transcript metadata, Nominatim address lookup, and OpenStreetMap pinning.
- Added focused tests for scanner transcription normalization and PII redaction.

### 0.6.2-beta-9

- Fixed `/api/v1/live-status` returning HTTP 500 after receiving non-finite audio samples.
- Sanitized live audio-level calculations so the endpoint always emits valid JSON numbers.
- Ignored blank alias names while preparing live scanner metadata.
- Added a final compatibility repair for non-finite levels already present in a live-status snapshot.

### 0.6.2-beta-8

- Fixed web API exceptions closing connections and appearing only as **Failed to fetch**.
- Isolated dashboard data requests so one failed panel no longer prevents the rest of the console from updating.
- Added endpoint-specific error details to the dashboard and application log.
- Snapshot live channel, tuner, broadcaster, destination, and alias collections during web requests to prevent concurrent-update failures.

### 0.6.2-beta-7

- Renamed the visible web interface header and browser title to **SDR-Trunk Web Console**.

### 0.6.2-beta-6

- Added the resolved talkgroup alias to recorded MP3/WAV filenames.
- Sanitized alias text using the existing recording filename safety and length handling.
- Preserved the talkgroup ID, source radio, timestamp, duplicate suffix, and recording format in filenames.

### 0.6.2-beta-5

- Expanded web Playlist Channels management with talkgroup and alias maintenance.
- Added manual talkgroup additions for supplementing RadioReference-imported systems.
- Added talkgroup recording, playback-priority, and per-Remote-Calls destination routing controls.
- Upgraded Remote Calls management from add-only to complete add, edit, enable/disable, and delete workflows.
- Exposed Remote Calls authentication, retry, concurrency, timeout, maximum-age, local Whisper, hosted Whisper, and translation settings.

### 0.6.2-beta-4

- Promoted the accumulated Raspberry Pi and web work to a distinct beta-4 release.
- Added the scanner-style receiving display and decoded activity history.
- Added core browser channel creation, editing, protocol/frequency changes, auto-start, start/stop, and deletion.
- Consolidated installation, security, feature, limitation, and release-history documentation.
- Expanded Live Traffic with talkgroup aliases, per-call metadata, audio dBFS level, and corrected headless CPU/memory reporting.

### 0.6.2-beta-3

- Added a self-contained Raspberry Pi Linux ARM64 distribution.
- Enabled desktop/VNC and headless operation from one package while keeping the web console available in both modes.
- Added first-run web-token creation, GUI token editing, and web-server restart controls.
- Added authenticated live call listening and recorded-audio playback.
- Added direct protocol changes and protocol selection while cloning Playlist Editor channels.

### Early beta work

- Added the embedded web server and initial status dashboard.
- Added the Remote Call API broadcast destination and completed MP3 upload.
- Added local and hosted Whisper transcription/translation processing.
- Added remote channel and streaming-destination status APIs.

## Building from source

```bash
./gradlew test
./gradlew runtimeZipRaspberryPi
./gradlew runtimeZipLinuxX86_64
```

The Raspberry Pi ARM64 and Linux x86_64 ZIPs are written under `build/image/`.

## Upstream project

sdrtrunk is a cross-platform Java application for decoding, monitoring, recording, and streaming trunked and related radio systems using software-defined radios.

- [Upstream getting started](https://github.com/DSheirer/sdrtrunk/wiki/Getting-Started)
- [Upstream user manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual)
- [Upstream support](https://github.com/DSheirer/sdrtrunk/wiki/Support)
- [Official upstream releases](https://github.com/DSheirer/sdrtrunk/releases)

This fork remains licensed under the GNU General Public License version 3 or later, consistent with the upstream project. See [LICENSE](LICENSE) for the license text.
