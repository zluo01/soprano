# Soprano

Soprano is an all-in-one music player server.  It mainly constructs with three parts
 - WebUI with [PWA](https://en.wikipedia.org/wiki/Progressive_web_app) support. ([screenshots](https://github.com/zluo01/soprano/tree/main/screenshots/mobile))
 - Music Management Server: written with graphql.
 - Playback server: integrated with MPV (internally use ffmpeg).

## Prerequisite
Soprano relies on three hard dependencies, **Java 21** for running the server, **mpv** and **ffmpeg** (Should come with mpv) for playback and image optimization.

### Java
You can follow guide for [Amazon Corretto OpenJDK](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/what-is-corretto-21.html) or install your own favorite JDK.

### MPV
#### MacOS

```
brew install mpv
```

#### Linux
##### Debian/Ubuntu
```
sudo apt install mpv libmpv-dev
```

##### Fedora/CentOs
```
sudo dnf instal mpv mpv-libs
```
#### Windows
> For Windows user, you need to add java, mpv and ffmpeg as environment variables such that they can be run directly in Command Prompt/Terminal.
> i.e. you can run `java -version` without any error

> Need to download both mpv and mpv-dev(libmpv)

https://mpv.io/installation/

#### Build From source

https://github.com/mpv-player/mpv-build


## Setup

 - Create file `soprano.properties` in config folder with content from [template](https://github.com/zluo01/soprano/blob/main/soprano.properties.template). 
 - Fill in properties.

### Configuration Path:
- Windows: `C:\Users\<username>\AppData\Roaming\soprano`
- Others: `$HOME/.config/soprano`


## Run
```
java -jar soprano-main.jar
```
You can access webUI through `<HOST_IP>:6868`.

## Initialization

To build the database from the provided music directory for the first time. You can either use `Build` or `Update`.  For any additional update, prefer use `Update`.

 - `Build`: Clear the database and covers and build the database from ground up. Since this will scan all files under the directory, will take much longer time compares to `Update`.
 - `Update`: Scan the directory and only update the database for miss match files (new or delete files)

## FAQ

**Q: `Non-C locale detected. This is not supported. Call 'setlocale(LC_NUMERIC, "C");' in your code.`**

**A:** Follow [this](https://askubuntu.com/a/724343) to fix.

**Q: Why there is no sound when I play music on Windows ?**

**A:** It is likely due to mpv try to stream audio to hardware without doing sampling/decoding and the output device does not support the format. Consider change the audio output device.


## Todo
 - P0: Add testing
 - P1: Auto refresh database on source folder change
 - P2: Graphql socket support
 - P2: Pad, Desktop web ui
