#!/bin/bash

# path to original web pages
ARCHIVE_PATH=/path/to/webis-web-archive-17/pages/
# path to reproduced web pages
REPRO_PATH=/path/to/webis-web-archive-17/pages/

# for all archived pages
for i in $(seq -f "%06g" 0 9999)
do
  # create video and motion vector files using ffmpeg script (old: $REPRO_PATH/$i/script/page.png)
  ./ffmpeg.sh $ARCHIVE_PATH/$i/archiving.png \
  $REPRO_PATH/$i/reproducing-custom.png
done
