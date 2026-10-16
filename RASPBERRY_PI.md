# Raspberry Pi desktop and web build

This distribution targets a Raspberry Pi 4 or 5 running a 64-bit Raspberry Pi OS (`aarch64`).  Java 27 and JavaFX
are included in the archive; Java does not need to be installed separately on the Pi.

## Install required system library

```bash
sudo apt update
sudo apt install libusb-1.0-0
```

## Install and run

Copy `sdr-trunk-linux-aarch64-v0.7.1-beta4.zip` to the Pi, then run:

```bash
unzip sdr-trunk-linux-aarch64-v0.7.1-beta4.zip
cd sdr-trunk-linux-aarch64-v0.7.1-beta4
export SDRTRUNK_WEB_PASSWORD='replace-with-a-long-random-password'
bin/sdr-trunk
```

`SDRTRUNK_WEB_PASSWORD` is optional. On the first desktop/VNC launch, sdrtrunk prompts you to create a password and saves
it. On the first headless launch, it creates and saves a random password and prints that password once in the startup log.
The saved password can be replaced while the desktop application is running from **View > Web Console Password**. An
`SDRTRUNK_WEB_PASSWORD` environment variable overrides the saved GUI setting; `SDRTRUNK_WEB_TOKEN` remains a legacy fallback.

The Playlist Editor also contains a **Web Interface** tab where you can save a replacement password and restart the web
server without restarting sdrtrunk. The web dashboard queues completed live radio calls, plays each one fully in
order, and applies the selected hold time before the next call. It also lists
the MP3/WAV files in the configured Audio Recordings directory for browser playback.  These audio endpoints require
the same web console password as the rest of the remote API.

The main page is a dedicated scanner-style display. When no call is active, its previous talkgroup, alias, frequency,
source, signal, and level are cleared. System status, Playlist, RadioReference, Recordings, Transcripts, Remote Calls,
and Settings open in modal windows from the top menu. The RadioReference modal supports Premium login, saved-location
restoration, Country/State/County/System browsing, selective talkgroup import, alias-list creation, and encrypted
talkgroup muting. Signal strength is shown as unavailable during an active call
when the selected tuner does not expose calibrated RSSI through sdrtrunk's common tuner interface. The web channel editor supports creating,
editing, starting/stopping, and deleting playlist channels. The top-menu RadioReference page can test Premium account
credentials and preview/import talkgroups by RadioReference system ID. Desktop location browsing remains available.

## Optional local Whisper transcription

Whisper is separate from the Java package. Install ffmpeg, Python, and Rob's Whisper fork on the Pi, preferably in a
virtual environment, then point **Settings > Whisper executable** in the web console at that environment's `whisper`
command. The first use downloads the selected model weights.

See the [complete local Whisper setup and troubleshooting guide](WHISPER_SETUP.md) for recommended models, service-user
permissions, and how sdrtrunk passes calls to the background process.

```bash
sudo apt install ffmpeg python3-venv
python3 -m venv "$PWD/whisper-env"
"$PWD/whisper-env/bin/pip" install git+https://github.com/robgwi/whisper.git
```

For Raspberry Pi hardware, try `tiny.en` first, then `base.en` if performance is acceptable. Enable background
transcription only after the executable path has been saved and tested. Transcript settings include the scanner
vocabulary prompt, spoken-number and phonetic cleanup, optional PII redaction, and a city/region hint for map pins.

When launched from a terminal in the Raspberry Pi desktop or VNC session, the Java desktop interface opens and the
web interface runs at the same time.  When no graphical display is available, start it explicitly in headless mode:

```bash
JAVA_OPTS='-Djava.awt.headless=true' bin/sdr-trunk
```

Open `http://<raspberry-pi-address>:10000/` from another computer. API clients must send the token as:

```text
Authorization: Bearer replace-with-a-long-random-token
```

The custom call destination key and hosted Whisper key can be supplied without putting them in the playlist:

```bash
export SDRTRUNK_REMOTE_API_KEY='destination-key'
export OPENAI_API_KEY='OpenAI-key'
```

For reliable operation, configure these environment variables in the service manager rather than a shell history.

## USB permissions

If the SDR is detected only when running as root, add an appropriate `udev` rule for the tuner instead of running
sdrtrunk as root.  The vendor and product IDs can be obtained with `lsusb`.
