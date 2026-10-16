# VCE Web Interface Integration

Version 0.7.1 Beta 1 replaces this fork's original inline dashboard with the browser interface from
[tylerwatt12/sdrtrunk-vce](https://github.com/tylerwatt12/sdrtrunk-vce). The Java desktop application, headless mode,
playlist format, decoders, streaming destinations, Remote Calls, and fork-specific features remain part of this
repository; only the browser presentation layer was replaced.

The imported interface was reviewed from VCE commit
[`ba099dc469298cebcac37e172a3db8ef647bcfec`](https://github.com/tylerwatt12/sdrtrunk-vce/commit/ba099dc469298cebcac37e172a3db8ef647bcfec)
(October 9, 2026).

This is a compatibility port, not a merge of the complete VCE receiver fork. The two Java codebases have different
upstream baselines and persistence models. Importing VCE's full server would remove or conflict with newer upstream
sdrtrunk code and several features in this fork. Instead, the VCE shell is served from `src/main/resources/web/vce`
and its supported pages call the existing embedded Java APIs through VCE-styled adapters.

## Supported pages

| VCE page | Backing API | Fork features retained |
| --- | --- | --- |
| Main | `/api/v1/status`, `/api/v1/activity`, `/api/v1/live-status`, `/api/v1/live-audio` | Scanner state, channel, talkgroup, alias, source, frequency, signal when available, sequential call playback, CPU and memory |
| Recordings | `/api/v1/recordings`, `/api/v1/recording-audio`, `/api/v1/transcripts` | MP3/WAV replay and Whisper transcript review |
| Channels | `/api/v1/channels`, `/api/v1/channel-options`, `/api/v1/talkgroups`, `/api/v1/channel-control` | Protocol editing, talkgroup aliases, native and Remote Call destination routing, per-channel event logging, NXDN unknown-simplex recording, NBFM carrier/CTCSS/DCS and tone search |
| RadioReference | `/api/v1/radioreference` | Premium login, geographic/system browsing, selective talkgroup import, duplicate detection and alias-list creation |
| Streaming | `/api/v1/broadcasters`, `/api/v1/remote-destinations` | Full native destination and Remote Call API editing, credentials, system IDs, state, queues, reconnect, clone, Rdio and Remote Call heartbeats, Broadcastify OTA/stored talker-alias selection and optional playlist storage, and transcription options |
| Tuners | `/api/v1/tuners` | Local tuner allocation/lock/health state, diagnostics, attached RTL-SDR frequency/sample-rate/PPM/Bias-T/R8x-gain editing, and complete rtl_tcp add/edit/enable/disable/reconnect/delete controls |
| Browser Spectrum | `/api/v1/spectrum` | Live spectrum and waterfall with FFT/window, zoom/pan, scale, smoothing, palette and update controls, active-channel overlays, cursor readout and confirmed idle-tuner retuning |
| Administration | `/api/v1/web-settings`, `/api/v1/whisper-settings`, `/api/v1/status`, `/api/v1/application-log`, `/api/v1/event-logs` | Password change, server restart, local Whisper setup/status, host metrics, application-log inspection, and event-log viewing/download |

The VCE client also uses these compatibility contracts:

- `GET /api/v1/auth/session`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me/preferences`
- `PUT /api/v1/me/preferences`

Authentication remains the fork's single administrator password. Successful login creates the existing expiring,
HTTP-only session cookie. Scripts and integrations may continue to send the same password as
`Authorization: Bearer <web-password>` to the fork APIs.

## Intentionally hidden in beta 25

VCE's multi-user roles, scan lists, SQLite call history and analytics, managed recording catalog, detailed receiver
health history, alias bulk tools, call-quality reports, live multiplex feed, and P25 network visualizer depend on VCE
backend services that are not present in this fork. Their navigation capabilities are disabled so the interface does
not offer controls that would fail or silently modify incompatible data.

The normal Java Playlist Editor remains available in desktop/VNC mode and is still the fallback for advanced
protocol-specific fields not exposed by the supported web channel editor.

## Licensing and source

The imported browser files retain the GNU GPL v3 license, project notice, and bundled third-party license notices in
`src/main/resources/web/vce`. The About page credits Tyler Watthanaphand and links to the VCE source. Changes in this
fork should not be reported as official sdrtrunk-vce behavior.
