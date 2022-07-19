#!/bin/bash

# path to reproduced web pages
REPRO_PATH=/path/to/reproduced/pages
# path to motion vector files
VIDEO_PATH=/path/to/h264/videos/mv

# for all pages
for i in $(seq -f "%06g" 0 9999)
do
  # detect shifts
  ./detect_shifts.sh "$REPRO_PATH/$i/elements.txt" \
    "$VIDEO_PATH/$i.mp4"
done
