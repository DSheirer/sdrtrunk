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
sdrtrunk code and several features in this fork. Instead, VCE's browser source is served from
`src/main/resources/web/vce` and its supported pages call the existing embedded Java APIs through compatibility
adapters. The original VCE branding, navigation, Main workspace, shared styles, and Administration settings shell are
retained. Fork controls are mounted inside those workspaces rather than replacing the browser with the earlier custom
scanner dashboard.

## Supported pages

| VCE page | Backing API | Fork features retained |
| --- | --- | --- |
| Main | `/api/v1/status`, `/api/v1/quality`, `/api/v1/dashboard`, `/api/v1/activity/actions`, `/api/v1/activity/radios` | Original VCE Main workspace, current channel/signal tiles, and graceful empty historical summaries when VCE's SQLite history service is absent |
| Recordings | `/api/v1/recordings`, `/api/v1/recording-audio`, `/api/v1/transcripts` | MP3/WAV replay and Whisper transcript review |
| Channels | `/api/v1/channels`, `/api/v1/channel-options`, `/api/v1/talkgroups`, `/api/v1/channel-control` | Source frequency/preferred tuner, protocol and traffic settings, NXDN channel/air-interface/talker-alias options, event logging and recorder selection, talkgroup aliases, native and Remote Call destination routing, NXDN unknown-simplex recording, NBFM carrier/CTCSS/DCS and tone search |
| RadioReference | `/api/v1/radioreference` | Premium login, geographic/system browsing, selective talkgroup import, duplicate detection and alias-list creation |
| Streaming | `/api/v1/broadcasters`, `/api/v1/remote-destinations` | Full native destination and Remote Call API editing, credentials, system IDs, state, queues, reconnect, clone, Rdio and Remote Call heartbeats, Broadcastify OTA/stored talker-alias selection and optional playlist storage, and transcription options |
| Tuners | `/api/v1/tuners` | Local tuner allocation/lock/health state, diagnostics, attached RTL-SDR frequency/sample-rate/PPM/Bias-T/R8x-gain editing, and complete rtl_tcp add/edit/enable/disable/reconnect/delete controls |
| Browser Spectrum | `/api/v1/spectrum` | Live spectrum and waterfall with FFT/window, zoom/pan, scale, smoothing, palette and update controls, active-channel overlays, cursor readout and confirmed idle-tuner retuning |
| Administration | `/api/v1/web-settings`, `/api/v1/users`, `/api/v1/whisper-settings`, `/api/v1/status`, `/api/v1/application-log`, `/api/v1/event-logs` | VCE Administration shell containing password, bind-interface and port configuration; active-listener status; administrator/operator account management; server restart; local Whisper setup/status; host metrics; and log inspection/download |

The VCE client also uses these compatibility contracts:

- `GET /api/v1/auth/session`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me/preferences`
- `PUT /api/v1/me/preferences`

Personal browser settings are validated against the current VCE schema and saved atomically in
`<application-root>/web_preferences.json`. An incompatible profile from an older beta is replaced with current defaults
instead of repeatedly displaying an unavailable-settings warning.

The built-in `admin` account uses the fork's existing administrator password. Administrators can add persistent named
administrator or operator accounts from either Playlist Editor > Web Interface or the browser Administration page.
Additional account passwords are salted and hashed with PBKDF2; successful login creates an expiring HTTP-only session
cookie. Scripts and integrations may continue to send the built-in administrator password as
`Authorization: Bearer <web-password>` to the fork APIs.

The embedded server saves its bind address and port. `127.0.0.1` permits only local browser access; select `0.0.0.0`
to listen on every IPv4 interface and use the computer's LAN address from another device. The active listener is shown
separately from the saved values. Explicit `-Dsdrtrunk.web.bind=...` and `-Dsdrtrunk.web.port=...` startup properties
remain authoritative and are identified as overrides in the interface.

## Intentionally hidden

VCE's scan lists, full SQLite call history, managed recording catalog, detailed receiver
health history, alias bulk tools, call-quality reports, live multiplex feed, and P25 network visualizer depend on VCE
backend services that are not present in this fork. Their navigation capabilities are disabled so the interface does
not offer controls that would fail or silently modify incompatible data.

The compatibility API does provide persistent recent-call activity, 24-hour call analytics, destination/source
rankings, radio-activity summaries, and source-radio drill-downs from SDRTrunk's retained decode events.

The normal Java Playlist Editor remains available in desktop/VNC mode and is still the fallback for specialized
protocol fields that do not yet have a web control, such as detailed trunked-system channel maps and decoder-specific
audio gain tuning.

## Licensing and source

The imported browser files retain the GNU GPL v3 license, project notice, and bundled third-party license notices in
`src/main/resources/web/vce`. The About page retains Tyler Watthanaphand's credit for the original VCE web interface
and credits Rob Gwi (`@robgwi`) for the SDRTrunk integration, GUI improvements, expanded APIs, and management features
in this fork. All of those modifications remain distributed under GNU GPL v3 or later. Changes in this fork should
not be reported as official sdrtrunk-vce behavior.
