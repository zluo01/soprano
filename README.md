# Soprano

Soprano is an all-in-one music control solution.  

## Prerequisite
Soprano relies on three hard dependencies, java for running the server, mpv and ffmpeg (Should come with mpv) for playback and image optimization.

### Java
You can follow guide for [Amazon Corretto OpenJDK](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/what-is-corretto-21.html) or install your own favoriate JDK.

### MPV
#### MacOS

```brew install mpv```

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
> For Windows user, you need to add java and ffmpg to path such that they can be run directly in Command Prompt/Terminal.

https://mpv.io/installation/

#### Build From source

https://github.com/mpv-player/mpv-build


## Setup
 - Create file `.confg/soprano/soprano.properties` with content from [template](https://github.com/zluo01/soprano/blob/main/soprano.properties.template). 
 - Fill in properties.

## Run
```
java -jar soprano-main.jar
```
You can access webUI through `<HOST_IP>:6868`.

## FAQ

If you see following error on start up, follow [this](https://askubuntu.com/a/724343) to fix.

```
Non-C locale detected. This is not supported.
Call 'setlocale(LC_NUMERIC, "C");' in your code.
```

## Todo
 - P0: Incremental update on database
 - P0: Add testing
 - P1: Auto refresh database on source folder change
 - P2: Pad, Desktop web ui
