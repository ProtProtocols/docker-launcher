# docker-launcher

A lightweight Java GUI to launch ProtProtocol images

<img src="./icon.svg" width="250">

## Introduction

This Java tool provides a simple graphical user interface to install and launch
ProtProtocol images.

ProtProtocol images are shipped as [Docker](https://www.docker.com) containers
that contain a complete analysis environment for common proteomics bioinformatics
workflows. Since [Docker](https://www.docker.com) containers are light-weight virtual
machines, all required software tools are installed by simply downloading the image.

We currently provide the following ProtProtocol images:

  * [IsoProt](https://github.com/ProtProtocols/IsoProt): This
    protocol supports the analysis of isobarically-labelled (TMT/iTRAQ) datasets starting
    from the identification of the spectra to the final statistical and differential
    expression analysis.

## Usage

### Installation

  * docker-launcher requires Java to be installed. You can download Java from http://www.java.com
  * You also need to have [Docker](https://www.docker.com) installed on your system.
    For help with this step see our [docker installation manual](./docs/installing_docker.md)
  * Download the current version from the [releases](https://github.com/ProtProtocols/docker-launcher/releases).
  * Unpack the zip file to any directory
  * Simply launch the docker-launcher-[VERSION].jar file (double click or use `java -jar /path/to/docker-launcher-[VERSION].jar)

## Changes

### Version 0.5

  * Updated to new ProtProtocols docker hub name
  * Added alerts for all critical errors

### Version 0.4

  * Available protocol image versions are automatically detected based on the GitHub tag versions.
  * Added controls to set input and output directory separately
  * Re-styled application
  * Added icon

### Version 0.3

  * Added support to launch image when Windows drive sharing fails
  * Detects if a new version is available for download (necessary as protocol versions are hard-coded)
  * Detects IP address of docker host when running Docker Toolbox

**Bugfixes:**

  * Fixed bug that prevented image download in Windows 10
  * Fixed bug that prevented file sharing to work with Docker Toolbox

### Version 0.2

  * Added functionality to select which image version to download and / or update

## Acknowledgements

<img src="flag_yellow_low.png" height="200" /><img src="LOGO-ERC.jpg" height="200" />

This project has received funding from the European Research Council (ERC) under the European Union's Horizon 2020 research and innovation programme under grant agreement No 788042.
