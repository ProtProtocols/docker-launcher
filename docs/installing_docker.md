# Docker Installation Instructions

Docker offers a free community edition of its virtualisation tool which can
be downloaded from the [Docker store](https://store.docker.com/search?type=edition&offering=community).

For **Windows 7** you need to install the [Docker toolbox](https://docs.docker.com/toolbox/toolbox_install_windows/)
instead of the Docker application from the store (see [Windows 7 - Docker toolbox](#Windows-7---Docker-toolbox)).

## Linux

Docker is native on linux. Most distributions already provide Docker in
their repositories. Alternatively, you can find Docker for various distributions
in the [Docker store](https://store.docker.com/search?type=edition&offering=community).

On **Ubuntu** Docker can be installed with this single commend:

```
$ sudo apt install docker.io
```

## Windows 10 - native application

Since Windows 10, Docker provides a "native" app which can be found
in the [Docker store](https://store.docker.com/search?type=edition&offering=community).

#### Setup for docker-launcher

Once you installed Docker using the setup file, you can find the Docker
icon in your taskbar.

<img src=https://docs.docker.com/docker-for-windows/images/whale-icon-systray-hidden.png" width="250" />

You can **access the settings** by right-clicking
this icon and clicking on "Settings".

<img src="https://docs.docker.com/docker-for-windows/images/docker-menu-settings.png" width="300" />

In order for **docker-launcher** to connect to
your docker instance, you need to `Expose daemon on tcp://localhost:2375 without TLS`.

<img src="https://docs.docker.com/docker-for-windows/images/settings-general.png" width="450" />

Finally, in order to be able to directly access your folders from the docker image (ie.
the ProtProtocols protocol) you need to share the respective drive with docker:

<img src="https://docs.docker.com/docker-for-windows/images/settings-shared-drives.png" width="450" />

You can find more information on the Docker settings on the [Docker homepage](https://docs.docker.com/docker-for-windows/#general).

## Windows 7 - Docker toolbox

Prior to Windows 10, Docker only supports Windows 7 through its [Docker toolbox](https://docs.docker.com/toolbox/toolbox_install_windows/).

Please follow the [official instructions](https://docs.docker.com/toolbox/toolbox_install_windows/) to
install it on your computer.

#### Setup for docker-launcher

[Docker toolbox](https://docs.docker.com/toolbox/toolbox_install_windows/) only allows access to
folders below `C:\Users` which includes your homedirectory, Desktop, Downloads etc. Access to any
other location is blocked. Therefore, you need to have / copy your MGF files either to a folder below
`C:\Users` (recommended option) or manually upload your MGF files into the ProtProtocol image through
the web interface.

**Note:** The same restriction also applies to the output folder!

