# Soprano

Soprano is an all-in-one music control solution.  

## Prerequisite
Soprano relies on two hard dependencies, mpv and ffmpeg (Should come with mpv).

### MacOS

`brew install mpv`

### Linux
#### Debian/Ubuntu
```
sudo apt install mpv libmpv-dev
```

#### Fedora/CentOs
```
sudo dnf instal mpv mpv-libs
```

### Build From source

https://github.com/mpv-player/mpv-build


## Setup
 - Create file `.confg/soprano/soprano.properties` with content from
 - Update required music directory

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
