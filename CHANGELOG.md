# Changelog

All notable changes in the `actionpagezello/sdrtrunk` fork are documented here. Upstream
DSheirer/sdrtrunk changes are not repeated; only the `ap-` fork deltas are recorded.

Versioning follows `0.6.2-ap-<n>` where `<n>` increments for each fork release.

## [0.6.2-ap-14.9.8.1] - 2026-05-20

Extended DCS code support.

### Added
- Extended the list of supported DCS codes from original 83 found in ETSI TS 103 236 V1.1.1 to the commonly used 104 codes supported in most modern day radios by calculating the Golay values, applying the offset and storing the values bit reversed in decimal notation. - by Paul Donahue (pauldonahue) with help of MS Copilot.

## [0.6.2-ap-14.6] - 2026-04-11

Runtime-diagnostics release. Adds per-category DEBUG toggles, persistent channel table sort,
and ThinLine Radio debug-by-default.

### Added
- Diagnostics preferences panel (Application -> Diagnostics (Logging)) with per-category
  DEBUG toggles for Zello, ThinLine Radio, Rdio Scanner, SDRPlay, RTL-SDR, the channelizer,
  the tuner manager, the P25 decoder, and NBFM/audio output.
- Master "Enable ALL diagnostics categories" checkbox with a warning about log volume.
- Runtime Logback level control (`LogLevelController`) that applies persisted preference
  state on startup and immediately when a checkbox is toggled, with no application restart.
- `FxTableColumnMonitor` helper that persists JavaFX TableView column widths, visible
  order, and sort order across restarts. Wired into the Channels editor with stable column
  ids (`channelTable.system`, `.site`, `.name`, `.frequency`, `.protocol`, `.playing`,
  `.autoStart`).
- ThinLine Radio logger defaults to DEBUG in `logback.xml` so live streaming sessions get
  full diagnostic output without user action.

### Fixed
- Channel table sort order and column widths are no longer lost when reopening the
  Channels menu. Previously the view reverted to the default ordering on every show.

### Changed
- `DiagnosticsPreference` is initialized by `UserPreferences` and immediately pushes its
  state into the Logback context via `LogLevelController.applyAll`, guaranteeing the user's
  last diagnostics selection is active from the first log line after startup.
- `.github/ISSUE_TEMPLATE/config.yml` now enables blank issues so fork issues can be filed
  without going through the upstream support wiki template.

## [0.6.2-ap-14.5] - 2026-04-10

- Treat "failed to start sending message" and "failed to stop sending message" as transient
  Zello server errors that trigger reconnect rather than broadcaster shutdown.
- Bumped `projectVersion` for the fix above.

## [0.6.2-ap-14.4] - 2026-04-09

- NBFM hiss reduction: wire new post-demod audio filters into the NBFM path.
- Default filter updates to reduce clipping on loud voice channels.
- Zello reconnect button on the broadcaster status panel for manual recovery.
- Verbose Zello diagnostic logging (session epoch, stream ids, opus state).
- Fix Opus encoder crash when frame size changed mid-session.
- Updated Zello broadcaster default configuration values.

## [0.6.2-ap-14.3] - 2026-04-07

- Earlier Zello reliability improvements (initial transient-error handling).
- Audio pipeline tuning for Cambridge COMIRS P25 trunking system.
- Rdio Scanner stream wiring and API-key reporting improvements.

[0.6.2-ap-14.6]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.6
[0.6.2-ap-14.5]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.5
[0.6.2-ap-14.4]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.4
[0.6.2-ap-14.3]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.3
