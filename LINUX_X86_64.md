# Linux x86_64 installation

This distribution supports 64-bit Intel and AMD Linux desktops, mini PCs, and headless servers. It includes its own
Java 27 and JavaFX runtime, so a separate Java installation is not required.

## Requirements

- A 64-bit Intel or AMD Linux installation (`uname -m` should report `x86_64`)
- A supported software-defined radio and its required system/driver packages
- `libusb-1.0-0` for USB tuner access

## Install

```bash
sudo apt update
sudo apt install libusb-1.0-0 unzip
unzip sdr-trunk-linux-x86_64-v0.7.1-beta5.zip
cd sdr-trunk-linux-x86_64-v0.7.1-beta5
bin/sdr-trunk
```

When launched from a desktop or VNC terminal, the Java desktop application and web console run together.

## Headless operation

```bash
JAVA_OPTS='-Djava.awt.headless=true' bin/sdr-trunk
```

The web console listens on port 10000. Open `http://<linux-computer-address>:10000/` from another computer.

On the first headless launch, sdrtrunk creates a random web console password, saves it, and writes it once to the startup
log. You can also provide one explicitly:

```bash
export SDRTRUNK_WEB_PASSWORD='replace-with-a-long-random-password'
JAVA_OPTS='-Djava.awt.headless=true' bin/sdr-trunk
```

API requests from another computer must include:

```text
Authorization: Bearer replace-with-a-long-random-password
```

## Local Whisper

The same [local Whisper setup guide](WHISPER_SETUP.md) applies to Linux x86_64. Install the Python Whisper environment
as the same account that runs sdrtrunk, then enter its absolute `whisper` executable path under **Settings > Whisper
Settings**. x86_64 systems are generally able to use larger models than a Raspberry Pi, depending on available CPU,
memory, and GPU support.
