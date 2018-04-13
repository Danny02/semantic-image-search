#!/bin/sh

BASEDIR=$(dirname "$0")
python3 "$BASEDIR"/classify_image.py "$@" | awk -F '::' '{system("exiftool -overwrite_original -iptc:keywords+='"'"'" $2 "'"'"' \"" $1 "\"")}'
