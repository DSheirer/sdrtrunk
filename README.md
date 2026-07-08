![Gradle Build](https://github.com/hydrasdr/sdrtrunk/actions/workflows/gradle.yml/badge.svg)
![Nightly Release](https://github.com/hydrasdr/sdrtrunk/actions/workflows/nightly.yml/badge.svg)

# sdrtrunk (HydraSDR fork)

A cross-platform java application for decoding, monitoring, recording and streaming trunked mobile and related radio protocols using Software Defined Radios (SDR).

This fork adds native [HydraSDR](https://github.com/hydrasdr) tuner support via a JNI bridge to [libhydrasdr](https://github.com/hydrasdr/hydrasdr-host), replacing the pure-Java USB implementation with ~25x lower streaming CPU overhead.

* [Download](https://github.com/hydrasdr/sdrtrunk/releases)
* [Build from source](BUILD.md)
* [Help/Wiki](https://github.com/DSheirer/sdrtrunk/wiki) (upstream)
* [User's Manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual) (upstream)
* [Upstream sdrtrunk](https://github.com/DSheirer/sdrtrunk) by Dennis Sheirer

## Download the Latest Release

All releases are available from the [releases](https://github.com/hydrasdr/sdrtrunk/releases) tab.

## Download Nightly Build

The [nightly](https://github.com/hydrasdr/sdrtrunk/releases/tag/nightly) release contains the latest builds for all supported platforms (Windows, Linux, macOS). It is updated on every push to master. **Always backup your playlist(s) before using nightly builds.**

## Minimum System Requirements
* **Operating System:** Windows 64-bit, Linux 64-bit, or macOS (64-bit, 12.x or higher)
* **CPU:** 4-core
* **RAM:** 8GB or more (preferred). 4GB may be sufficient depending on usage.
