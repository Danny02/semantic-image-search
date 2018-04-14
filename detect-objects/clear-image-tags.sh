#!/bin/sh

exiftool -overwrite_original -iptc:keywords= "$@"