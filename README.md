# Soprano

Soprano turns the music collection on your computer into a clean, easy-to-use listening experience. Point it at your music folder, and it organizes your songs, albums, artists, genres, and cover art for you. Open the web app from a phone, tablet, or desktop to explore your collection and control playback—no separate client app required.

### What you can do

- **Explore your collection** — browse recently added music, albums, artists, album artists, and genres with a visual, cover-focused interface.
- **Find and play music quickly** — search for songs, albums, or artists, play individual tracks or full albums, and manage what plays next.
- **Create your own playlists** — collect favorites, build playlists, and return to them whenever you want.
- **Control music from any screen** — use the responsive web app in a browser or install it on your device for an app-like experience.
- **Build your own integrations** — use Soprano's open [GraphQL API](src/main/resources/schemas/main.graphql) to create custom clients and automations.

[View mobile screenshots](screenshots/mobile)

## Installation

### 1. Download Soprano

Download the version that matches your computer from the [Releases page](https://github.com/zluo01/MESA/releases) and extract it. Each download is a standalone app with everything needed to run Soprano.

| Platform | Download |
| --- | --- |
| Windows, 64-bit | `soprano-win32-x86-64` |
| macOS, Apple silicon | `soprano-darwin-aarch64` |
| Linux, Intel/AMD 64-bit | `soprano-linux-x86-64` |
| Linux, ARM64 | `soprano-linux-aarch64` |

### 2. Connect your music library

Before starting Soprano for the first time, create a file named `soprano.properties` in the location for your platform:

| Platform | Configuration file |
| --- | --- |
| Windows | `%APPDATA%\soprano\soprano.properties` |
| macOS | `~/Library/Application Support/soprano/soprano.properties` |
| Linux | `$XDG_DATA_HOME/soprano/soprano.properties`, or `~/.local/share/soprano/soprano.properties` when `$XDG_DATA_HOME` is not set |

Create the parent folder if it does not already exist. Most users only need to add the absolute path to their music folder:

```properties
directory.music=/absolute/path/to/your/music
```

On Windows, forward slashes can be used in the path—for example:

```properties
directory.music=C:/Users/your-name/Music
```

See the [full configuration template](soprano.properties.template) for optional settings such as logging, cover sizes, and audio output.

### 3. Start Soprano

On Windows, open `soprano.exe`. On macOS or Linux, open a terminal in the extracted folder and run:

```shell
chmod +x soprano
./soprano
```

Keep Soprano running on the computer that stores your music and plays the audio.

### 4. Open the web app

Open `http://localhost:6868` on the same computer. From another phone, tablet, or computer on your network, open `http://<computer-ip>:6868`.

### 5. Scan your music library

When Soprano opens for the first time, the library will be empty. Open **Settings** and select the update button to scan the music folder configured above.

The first scan imports your songs, reads their metadata, and prepares album artwork. It may take a while for a large collection. Run the update again whenever you add or remove music files.

## Development

### Requirements

To build Soprano from source, install:

- **JDK 25**
- **Maven**
- **Git, curl, Python 3, and a C/C++ build toolchain** for the bundled native libraries
- **Node.js 24 and pnpm 11** only when running the frontend development server directly. Maven downloads its own pinned Node.js and pnpm versions for normal builds.

The native build scripts do not support repository paths containing spaces.

### Build the native libraries

Soprano bundles platform-specific playback and image-processing libraries. Build them once before running or packaging the application. The generated files are placed in `native/out/resources`.

#### Linux

Debian or Ubuntu:

```shell
sudo apt install build-essential ninja-build pkg-config git curl python3-venv libasound2-dev
./native/build-linux.sh
```

Fedora:

```shell
sudo dnf install gcc gcc-c++ make ninja-build pkgconf git curl python3 alsa-lib-devel
./native/build-linux.sh
```

The Linux script builds for the architecture of the current machine.

#### macOS

Install the Xcode Command Line Tools and ensure Python 3 is available:

```shell
xcode-select --install
./native/build-mac.sh
```

The macOS script builds for the architecture of the current Mac.

#### Windows

The Windows native libraries are cross-compiled from Linux or WSL with MinGW-w64:

```shell
sudo apt install build-essential ninja-build pkg-config git curl python3-venv gcc-mingw-w64-x86-64 g++-mingw-w64-x86-64
./native/build-windows.sh
```

To produce a native Windows `.exe`, use the generated `native/out/resources` files when running the Maven native build with a Windows GraalVM installation. The CI workflow follows this process automatically.

### Build and test

Run the complete backend and frontend checks:

```shell
mvn verify
```

This compiles the Java backend, installs the pinned frontend tools, checks and builds the web interface, and runs the test suite.

### Run locally with frontend hot reload

Create `soprano.properties` as described in the installation section, then start the backend from the repository root:

```shell
mvn compile exec:java
```

In a second terminal, start the Vite development server:

```shell
cd frontend
pnpm install
pnpm dev
```

Open `http://localhost:5173`. The development server forwards GraphQL, WebSocket, and cover-image requests to the backend on port `6868`.

### Build runnable packages

To build a runnable Java bundle:

```shell
mvn -Puber package
java -jar target/soprano-main.jar
```

The Java bundle requires JDK 25 on the computer where it runs.

To build the standalone native executable used for releases, install GraalVM Community Edition 25 with Native Image and run:

```shell
mvn -Pnative package
```

The executable is written to `target/soprano` on Linux and macOS, or `target/soprano.exe` on Windows. Native libraries must be built for the same platform and architecture as the executable.

## FAQ

**Q: How can I verify that playback is bit-perfect and is not being resampled?**

**A:** There is no single software indicator that proves every output bit is unchanged. A practical test is to play lossless tracks with known sample rates—such as 44.1, 48, and 96 kHz—and confirm that the audio device opens at the same rate for each track.

Before testing, set software and device volume to 100%, disable audio enhancements or DSP, and restart Soprano after changing `soprano.properties`.

**Linux**

Use a direct ALSA hardware device so playback does not pass through `default`, `plughw`, PipeWire, PulseAudio, or another software mixer. Find the card and device numbers with:

```shell
aplay -l
```

Then configure Soprano, replacing `2,0` with the card and device numbers for your output:

```properties
audio.hardware=alsa/hw:2,0
audio.options.override=alsa-resample=no
```

While a track is playing, inspect the parameters accepted by the hardware:

```shell
cat /proc/asound/card2/pcm0p/sub0/hw_params
```

The `rate` value should match the track's sample rate. The reported sample format may be wider than the source—for example, a 16-bit track may be carried as `S32_LE` with padding—without being resampled. If the file reports `closed`, check that the card, device, and subdevice numbers match the active output.

For example, the following results show the hardware switching correctly between a 16-bit/44.1 kHz track and a 24-bit/96 kHz track:

```console
$ cat /proc/asound/card*/pcm*p/sub*/hw_params
closed
closed
access: RW_INTERLEAVED
format: S16_LE
subformat: STD
channels: 2
rate: 44100 (44100/1)
period_size: 882
buffer_size: 4410

$ cat /proc/asound/card*/pcm*p/sub*/hw_params
closed
closed
access: RW_INTERLEAVED
format: S24_LE
subformat: STD
channels: 2
rate: 96000 (96000/1)
period_size: 2400
buffer_size: 9600
```

The `closed` entries are inactive playback devices or substreams and can be ignored. The active output reports the expected sample format and rate for each track.

**macOS**

Enable exclusive CoreAudio output:

```properties
audio.options.override=audio-exclusive=yes
```

Open **Audio MIDI Setup**, choose **Window → Show Audio Devices**, and select the output device. While playing test tracks, confirm that the sample rate shown under **Format** changes to match each track. An external DAC's sample-rate display is an even clearer confirmation. Exclusive mode uses direct device access, selects a matching hardware format, and prevents the normal system mixer from sharing the device during playback.

**Windows**

Open **Settings → System → Sound → More sound settings**. Select the playback device, then under **Properties → Advanced** enable:

- **Allow applications to take exclusive control of this device**
- **Give exclusive mode applications priority**

Also turn off audio enhancements and spatial audio for the device. Then enable WASAPI exclusive output in Soprano:

```properties
audio.options.override=audio-exclusive=yes
```

Play tracks with different known sample rates and check the display or control panel supplied with your DAC or audio interface. It should switch to the rate of each track. The **Default Format** shown in Windows Sound settings applies to shared mode and is not a reliable live indicator while Soprano is using exclusive mode. As a secondary check, other applications should be unable to use that output device while Soprano is playing.

Matching the hardware sample rate confirms that sample-rate conversion is not occurring at the final output stage. Strict bit-for-bit verification additionally requires a hardware loopback or audio analyzer, because matching sample rates alone cannot detect volume changes or other processing.

**Q: `Non-C locale detected. This is not supported. Call 'setlocale(LC_NUMERIC, "C");' in your code.`**

**A:** Follow [this](https://askubuntu.com/a/724343) to fix.
