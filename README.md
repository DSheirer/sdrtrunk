## Custom Additions in This Fork

This fork adds the following features on top of the upstream SDRTrunk release. All settings are configured inside SDRTrunk's native Preferences dialog under **View → Preferences → External Outputs** — no config files or manual editing required.

> **Build requirement:** This fork requires [Bellsoft Liberica JDK 25 Full](https://bell-sw.com/pages/downloads/#jdk-25) to build and run. The "Full" edition bundles JavaFX, which standard OpenJDK does not include. Build with `.\gradlew.bat clean distZip` on Windows.

---

### System Event Logging
Writes all control and traffic channel decode events to daily rolling CSV files — one file per trunking system, named `{SYSTEM}_YYYY-MM-DD_events.csv`. Files roll over at midnight and append on restart so no data is lost. Enable per-channel under **Playlist Editor → Logging → System Events Log**.

### Talker Alias Logging
Persists the active radio alias map to a CSV state file (`{SYSTEM}_talker_aliases.csv`). Aliases accumulate across restarts — the file is never reset to zero. New over-the-air aliases are merged on top of previously saved ones automatically.

### Control Channel Heartbeat Monitor
Fires throttled HTTP GET pings to configured endpoints (e.g. Uptime Kuma, or any HTTP push API) whenever a P25 Phase 1 control channel is actively receiving RFSS Status Broadcast messages. If the channel goes silent — antenna problem, SDR disconnect, coverage gap — the pings stop and your uptime monitor alerts you.

Configure under **View → Preferences → External Outputs → Heartbeat Monitor**:

![Heartbeat Monitor preferences panel](docs/images/heartbeat_monitor.png)

### TCP Network Streaming
Streams all decode activity as live newline-delimited JSON (NDJSON) over TCP. Multiple clients can connect simultaneously with no polling delay. Two ports:

- **Port 9500 — Event stream:** Every call event (GROUP_CALL, DATA_CALL, etc.) from all P25 and DMR channels, tagged with system name
- **Port 9501 — Raw CC stream:** Every valid decoded control-channel message (TSBKs, CSBKs, AMBTCs) before SDRTrunk processes it into a higher-level event. DMR voice frames are filtered out — signaling only.

Configure under **View → Preferences → External Outputs → Network Stream**:

![Network Stream preferences panel](docs/images/network_stream.png)

All existing CSV logging continues unchanged — TCP streaming is purely additive.

---

![Gradle Build](https://github.com/dsheirer/sdrtrunk/actions/workflows/gradle.yml/badge.svg)
![Nightly Release](https://github.com/dsheirer/sdrtrunk/actions/workflows/nightly.yml/badge.svg)

# MacOS Tahoe 26.1 Users - Attention:
Changes to USB support in Tahoe version 26.x cause sdrtrunk to fail to launch.  Do the following to install the latest libusb and create a symbolic link and then use the nightly build which includes an updated usb4java native library for Tahoe with ARM processor.  There may still be issue(s) with MacOS accessing your USB SDR tuners.

```
brew install libusb --HEAD
cd /opt
sudo mkdir local
cd local
sudo mkdir lib
```
Next, find where brew installed the libusb library, for example: ```/opt/homebrew/Cellar/libusb/HEAD-9ceaa52/lib/libusb-1.0.0.dylib```    Note: the folder "HEAD-9ceaa52" is the version stamp for HEAD when you installed from it.

Finally, create a symbolic link from the installed library to the place where usb4java is expecting to find libusb (/opt/local/lib/libusb-1.0.0.dylib)

```
sudo ln -s /opt/homebrew/Cellar/libusb/HEAD-9ceaa52/lib/libusb-1.0.0.dylib /opt/local/lib/libusb-1.0.0.dylib
```

# sdrtrunk
A cross-platform java application for decoding, monitoring, recording and streaming trunked mobile and related radio protocols using Software Defined Radios (SDR).

* [Help/Wiki Home Page](https://github.com/DSheirer/sdrtrunk/wiki)
* [Getting Started](https://github.com/DSheirer/sdrtrunk/wiki/Getting-Started)
* [User's Manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual)
* [Download](https://github.com/DSheirer/sdrtrunk/releases)
* [Support](https://github.com/DSheirer/sdrtrunk/wiki/Support)

![sdrtrunk Application](https://github.com/DSheirer/sdrtrunk/wiki/images/sdrtrunk.png)
**Figure 1:** sdrtrunk Application Screenshot

## Download the Latest Release
All release versions of sdrtrunk are available from the [releases](https://github.com/DSheirer/sdrtrunk/releases) tab.

* **(alpha)** These versions are under development feature previews and likely to contain bugs and unexpected behavior.
* **(beta)** These versions are currently being tested for bugs and functionality prior to final release.
* **(final)** These versions have been tested and are the current release version.

## Download Nightly Software Build
The [nightly](https://github.com/DSheirer/sdrtrunk/releases/tag/nightly) release contains current builds of the software 
for all supported operating systems.  This version of the software may contain bugs and may not run correctly.  However, 
it let's you preview the most recent changes and fixes before the next software release.  **Always backup your 
playlist(s) before you use the nightly builds.**  Note: the nightly release is updated each time code changes are 
committed to the code base, so it's not really 'nightly' as much as it is 'current'.

## Minimum System Requirements
* **Operating System:** Windows (~~32 or~~ 64-bit), Linux (~~32 or~~ 64-bit) or Mac (64-bit, 12.x or higher)
* **CPU:** 4-core
* **RAM:** 8GB or more (preferred).  Depending on usage, 4GB may be sufficient.
