# docker-launcher
A lightweight Java GUI to launch ProtProtocol images

## Introduction

This Java tool provides a simple graphical user interface to install and launch
ProtProtocol images.

## Usage

### Installation

  * docker-launcher requires Java to be installed. You can download Java from http://www.java.com
  * Download the current version from the [releases](https://github.com/ProtProtocols/docker-launcher/releases). 
  * Unpack the zip file to any directory
  * Simply launch the docker-launcher-[VERSION].jar file (double click or use `java -jar /path/to/docker-launcher-[VERSION].jar)

## Changes

### Version 0.3

  * Added support to launch image when Windows drive sharing fails
  * Detects if a new version is available for download (necessary as protocol versions are hard-coded)
  * Detects IP address of docker host when running Docker Toolbox

**Bugfixes:**

  * Fixed bug that prevented image download in Windows 10
  * Fixed bug that prevented file sharing to work with Docker Toolbox

### Version 0.2

  * Added functionality to select which image version to download and / or update