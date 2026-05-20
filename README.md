## Custom Additions in This Fork

This fork adds the following features on top of the upstream SDRTrunk release. All settings are configured inside SDRTrunk's native Preferences dialog under **View → Preferences → External Outputs** — no config files or manual editing required.

> **Build requirement:** This fork requires [Bellsoft Liberica JDK 25 Full](https://bell-sw.com/pages/downloads/#jdk-25) to build and run. The "Full" edition bundles JavaFX, which standard OpenJDK does not include. Build with `.\gradlew.bat clean distZip` on Windows.

---

### System Event Logging
Everything that appears in the channel Events tab can now be saved to disk or streamed over the network. Both options are independent and can be used together.

Enable CSV logging and events are written to daily rolling files (`{SYSTEM}_YYYY-MM-DD_events.csv`), one per trunking system, rolling at midnight and appending on restart. Enable TCP streaming and the same events flow out live in real-time. Configure per-channel under **Playlist Editor → Logging → System Events Log**.

### Talker Alias Logging
By default, SDRTrunk does not save talker alias data to disk — aliases are only held in memory and lost when the application closes. This fork changes that: talker alias data is now automatically captured from every monitored system and written to a persistent CSV state file (`{SYSTEM}_talker_aliases.csv`) with no configuration required.

Aliases accumulate across restarts — the file is never reset to zero. Each time SDRTrunk starts, previously saved aliases are silently reloaded so your alias count only ever grows. New over-the-air aliases received during a session are merged on top, and if a radio transmits a different alias than what was previously recorded, the newer value takes over automatically.

### Control Channel Heartbeat Monitor
Fires throttled HTTP GET pings to configured endpoints (e.g. Uptime Kuma, or any HTTP push API) whenever a P25 Phase 1 control channel is actively receiving RFSS Status Broadcast messages. If the channel goes silent — antenna problem, SDR disconnect, coverage gap — the pings stop and your uptime monitor alerts you.

Configure under **View → Preferences → External Outputs → Heartbeat Monitor**:

![Heartbeat Monitor preferences panel](https://github.com/user-attachments/assets/ba7debd2-fe99-4a26-ac51-f53b055988de)

### TCP Network Streaming
Streams all decode activity as live newline-delimited JSON (NDJSON) over TCP. Multiple clients can connect simultaneously with no polling delay. Two ports:

- **Port 9500 — Event stream:** Everything you see in SDRTrunk's Events tab — group calls, individual calls, data calls, affiliations, registrations, site updates — from all P25 and DMR channels, delivered in real-time as JSON. Each message is tagged with the system name so a single connected client can receive all your monitored systems at once and filter from there.
- **Port 9501 — Raw CC stream:** Every valid decoded control-channel message (TSBKs, CSBKs, AMBTCs) before SDRTrunk processes it into a higher-level event. DMR voice frames are filtered out — signaling only.

Configure under **View → Preferences → External Outputs → Network Stream**:

![Network Stream preferences panel](https://github.com/user-attachments/assets/0cd0b841-09a0-4c61-af70-52f14a595f90)

All existing CSV logging continues unchanged — TCP streaming is purely additive.

### IMBE Audio Stream
Streams raw compressed voice frames from every active P25 Phase 1 voice channel over TCP in real-time, allowing any application on your network to decode and play live audio without waiting for a call to end and an MP3 file to be written. Latency is ~20ms — the time between a radio keying up and audio arriving at the consumer.

When enabled, SDRTrunk opens a TCP server on the configured port (default 9502) and streams one JSON line per IMBE voice frame as calls happen. Each frame is tagged with the talkgroup, source radio unit, and a unique call ID so consumers can handle multiple simultaneous calls cleanly. The JMBE library used to decode the frames is the same open-source library SDRTrunk uses internally — audio quality is identical.

Three message types flow on the stream:
- **`call_start`** — emitted when squelch opens; carries talkgroup, source unit ID, system name, and a unique call ID
- **`frame`** — one per 18-byte IMBE voice frame (~9 per LDU burst, one every ~20ms); carries Base64-encoded vocoder data and a sequence number for dropped-frame detection
- **`call_end`** — emitted when squelch closes; carries the call ID and total frame count for the transmission

Multiple clients can connect simultaneously — each receives a full copy of the stream. Filter client-side by `talkgroup` or `system` to isolate specific channels. The `seq` field increments per call, so a gap within the same `callId` indicates lost frames.

Configure under **View → Preferences → External Outputs → IMBE Audio Stream**. The preference panel includes full protocol documentation and working code examples in Java and Python.

All existing MP3 recording continues unchanged — IMBE streaming is purely additive.

<img width="1180" height="770" alt="Screenshot 2026-05-20 124555" src="https://github.com/user-attachments/assets/a78ca1a1-f1d7-4a73-b520-1368b204c890" />


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
