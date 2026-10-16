# Local Whisper setup for sdrtrunk

This guide installs [Rob's Whisper fork](https://github.com/robgwi/whisper) beside sdrtrunk and connects it to the
web console's background transcription service. Whisper is optional and is not included in the sdrtrunk ZIP because
Python, PyTorch, ffmpeg, and the model files are large and platform-specific.

The local workflow does not require an OpenAI API key. Audio stays on the Raspberry Pi, except for the one-time model
download.

## How the connection works

sdrtrunk does not import Whisper into Java or contact GitHub for each call. After a configured, record-enabled call is
complete, sdrtrunk:

1. Encodes the call as a temporary MP3.
2. Starts the installed `whisper` command as a separate background process.
3. Supplies the model, language, task, and scanner vocabulary configured in the web console.
4. Reads Whisper's text output and applies optional scanner-number normalization and PII redaction.
5. Shows the result below the live scanner and in the **Transcripts** window.
6. Deletes the temporary MP3 and text files.

Calls are transcribed one at a time so Whisper cannot consume all CPU and memory while sdrtrunk is decoding.

## Requirements

- Raspberry Pi 4 or 5 with a 64-bit operating system (`uname -m` should report `aarch64`)
- Internet access during installation and the first use of each model
- At least several gigabytes of free disk space for Python, PyTorch, and model files
- A talkgroup alias with **Record calls** enabled in sdrtrunk

## 1. Install system packages

Open a terminal on the Pi:

```bash
sudo apt update
sudo apt install git ffmpeg python3-venv
```

Confirm ffmpeg is available:

```bash
ffmpeg -version
```

## 2. Install the Whisper fork

Install Whisper as the same Linux user that runs sdrtrunk. The following example places it in your home directory:

```bash
python3 -m venv "$HOME/whisper-env"
"$HOME/whisper-env/bin/pip" install --upgrade pip
"$HOME/whisper-env/bin/pip" install git+https://github.com/robgwi/whisper.git
```

Verify the command:

```bash
"$HOME/whisper-env/bin/whisper" --help
```

If this command fails, correct the Python installation before configuring sdrtrunk.

## 3. Configure sdrtrunk

1. Start sdrtrunk and open `http://<raspberry-pi-address>:10000/`.
2. Select **Settings** from the top menu.
3. Set **Whisper executable** to the full path shown by:

   ```bash
   realpath "$HOME/whisper-env/bin/whisper"
   ```

   Copy only the output of that command into sdrtrunk, for example
   `/home/username/whisper-env/bin/whisper`. Do not copy the word `realpath`, quotation marks, `$HOME`, or the
   terminal command itself into the setting.

4. Start with these settings:

   | Setting | Recommended Raspberry Pi value |
   | --- | --- |
   | Model | `tiny.en` |
   | Language | `English` |
   | Task | `transcribe` |
   | Timeout | `300` seconds |
   | Background transcription | Enabled |
   | Normalize scanner numbers | Enabled |
   | Redact PII | Enable if transcripts may be viewed by others |

5. Save the settings.

The first call can take substantially longer because Whisper downloads the selected model. After it finishes, the
transcript appears under **Live Traffic Scanner** and in **Transcripts**.

## Model selection

| Model | Raspberry Pi guidance |
| --- | --- |
| `tiny.en` | Best starting point for English radio traffic; fastest and lowest memory use |
| `base.en` | More accurate, but slower and more memory-intensive |
| `tiny` or `base` | Use for non-English transcription or translation to English |
| Larger models | Usually impractical while the same Pi is performing real-time SDR decoding |

Models ending in `.en` are English-only. To translate non-English audio into English, select `tiny` or `base`, set the
language appropriately, and choose the `translate` task.

## Running sdrtrunk as a service

The executable path must be accessible to the account running the service. If sdrtrunk runs as a different user,
install Whisper for that user or give that user execute/read access to the virtual environment and its model cache.
Always enter the absolute executable path in the web settings; a service may not have the same `PATH` as an interactive
terminal.

Test using the service account before starting sdrtrunk:

```bash
sudo -u SDRTRUNK_USER /home/SDRTRUNK_USER/whisper-env/bin/whisper --help
sudo -u SDRTRUNK_USER ffmpeg -version
```

Replace `SDRTRUNK_USER` with the actual Linux account name.

## What gets transcribed

Beginning with sdrtrunk `0.7.0-beta-5`, the web transcription path only accepts calls that meet all of these rules:

- The destination talkgroup ID matches an alias in the channel's assigned alias list.
- **Record calls** is enabled for that talkgroup alias.
- The call is not encrypted.
- The alias is not set to **Do Not Monitor**.
- The call is not rejected by duplicate-call suppression.

Background transcription runs even when no browser is open and does not require **Start Live Listening** to be active.

## Troubleshooting

### The dashboard says Whisper is disabled

Open **Settings**, enable **Background transcription**, and save.

If Whisper is not installed on this machine, leave **Background transcription** disabled. Whisper is optional and
sdrtrunk will continue decoding, recording, streaming, and serving the web console without it.

### The dashboard says Whisper is unavailable

The executable field does not point to a runnable Whisper installation. Enter either `whisper` when it is available
on the sdrtrunk process's `PATH`, or an absolute path such as `/home/username/whisper-env/bin/whisper`. Beginning with
beta 25, unavailable configurations are shown in Settings and are skipped without producing an error for every call.

### A call plays but no transcript appears

- Confirm the talkgroup alias has **Record calls** enabled.
- Confirm the executable is an absolute path such as `/home/username/whisper-env/bin/whisper`.
- Run that exact executable with `--help` as the account running sdrtrunk.
- Confirm `ffmpeg -version` works for the same account.
- Allow extra time for the first model download.
- Raise the timeout to 600 seconds for initial testing.
- Check the sdrtrunk log for `Background Whisper transcription failed` and the underlying command error.

### The process times out or decoding becomes unreliable

Use `tiny.en`, increase the timeout, and avoid larger models. Whisper is serialized to one worker, but inference can
still compete with SDR decoding for CPU and memory.

### Transcripts disappear after restarting

The current web transcript history is held in memory and keeps the latest 200 entries. Restarting sdrtrunk clears that
history; recorded audio files remain on disk.

## Python Whisper versus Remote Calls local Whisper

The main **Settings > Whisper Settings** feature described here uses the Python `whisper` command installed from
`robgwi/whisper`. The **Remote Calls > Local Whisper** fields currently use the `whisper.cpp` command-line format and
are a separate integration. Do not enter the Python executable in the Remote Calls local-Whisper field.

Hosted OpenAI `whisper-1` is also separate. It sends audio to the OpenAI API and requires an API key; the local setup
in this guide does not.
