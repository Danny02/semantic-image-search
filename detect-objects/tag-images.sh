#!/bin/sh

BASEDIR=$(dirname "$0")
python3 "$BASEDIR"/detect_objects.py $@ | awk -F '::' '{system("exiftool -overwrite_original -iptc:keywords='"'"'" $2 "'"'"' " $1)}'
