# Building sdrtrunk with HydraSDR JNI

## Clone

```bash
git clone https://github.com/hydrasdr/sdrtrunk.git
cd sdrtrunk
```

Gradle is bundled in the repository (`./gradlew` on Linux/macOS,
`gradlew.bat` on Windows). It downloads itself on first run — no
separate Gradle installation needed.

## Windows MINGW64 (GCC)

### Prerequisites

- [Git for Windows](https://git-scm.com/download/win)
- [BellSoft Liberica JDK 25 Full](https://bell-sw.com/pages/downloads/#jdk-25) (includes JavaFX)
- [MSYS2](https://www.msys2.org/) with mingw-w64 toolchain:
  ```
  pacman -S mingw-w64-x86_64-cmake mingw-w64-x86_64-ninja mingw-w64-x86_64-gcc
  ```

### Build

From MSYS2 MINGW64 shell:

```bash
export JAVA_HOME="/c/Program Files/BellSoft/LibericaJDK-25-Full"
export PATH="/d/msys64/mingw64/bin:/d/msys64/usr/bin:$JAVA_HOME/bin:$PATH"
./gradlew clean runtimeZipCurrent -Ptoolchain=mingw64
```

## Windows Visual Studio 2022 (MSVC)

### Prerequisites

- [Git for Windows](https://git-scm.com/download/win)
- [BellSoft Liberica JDK 25 Full](https://bell-sw.com/pages/downloads/#jdk-25) (includes JavaFX)
- [Visual Studio 2022](https://visualstudio.microsoft.com/) with "Desktop development with C++" workload (includes CMake)
- [Ninja](https://ninja-build.org/) — install via [Chocolatey](https://chocolatey.org/install): `choco install ninja`, or download from [ninja-build.org](https://github.com/ninja-build/ninja/releases)
- [7-Zip](https://www.7-zip.org/) (for automatic libusb extraction)

### Build

From a **x64 Native Tools Command Prompt for VS 2022**:

```cmd
set JAVA_HOME="C:\Program Files\BellSoft\LibericaJDK-25-Full"
gradlew clean runtimeZipCurrent -Ptoolchain=vs2022
```

## Linux (Ubuntu/Debian)

### Prerequisites

```bash
sudo apt-get install -y git build-essential cmake ninja-build libusb-1.0-0-dev
```

- [BellSoft Liberica JDK 25 Full](https://bell-sw.com/pages/downloads/#jdk-25) (includes JavaFX)

### Build

```bash
export JAVA_HOME=/path/to/liberica-jdk-25-full
./gradlew clean runtimeZipCurrent
```

## macOS

### Prerequisites

- Xcode Command Line Tools: `xcode-select --install`
- [Homebrew](https://brew.sh/) packages:
  ```
  brew install cmake ninja libusb
  ```
- [BellSoft Liberica JDK 25 Full](https://bell-sw.com/pages/downloads/#jdk-25) (includes JavaFX)

### Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew clean runtimeZipCurrent
```

## Output

Self-contained runtime image with bundled JRE:

```
build<_toolchain>/image/sdr-trunk-<platform>-<version>/
  bin/          <- JRE + launcher
  lib/native/   <- hydrasdr_jni, hydrasdr, libusb (Windows)
```

Example: `build_mingw64/image/sdr-trunk-windows-x86_64-v0.6.2-beta-1/`

Run: `bin/sdr-trunk` (Windows: `bin\sdr-trunk.bat`)

## Notes

- `-Ptoolchain=<name>` isolates the entire build per toolchain:
  `jni/build_<name>/` for native libs, `build_<name>/` for Gradle output.
  MINGW64 and VS2022 builds coexist without conflicts. Without this flag,
  the defaults `jni/build/` and `build/` are used (CI default).
- Gradle automatically runs cmake + ninja if native libs are not already
  present. CMake auto-clones [libhydrasdr](https://github.com/hydrasdr/hydrasdr-host)
  from GitHub via FetchContent.
- If cmake, ninja, or a C compiler are not available, the native build
  is skipped with a warning. The Java application still builds but
  HydraSDR tuner support will not be available.
- CI/CD (`.github/workflows/nightly.yml`) pre-builds native libs on 5
  platforms in parallel, then packages with Gradle. The `buildHydraSdrJni`
  task detects pre-built libs and skips.
