#!/bin/bash

# path to reproduced web pages
REPRO_PATH=/path/to/webis-web-archive-17/pages/

# for all pages
for i in $(seq -f "%06g" 0 9999)
do
  # compare original and archived image
  ./compare_images.sh "$REPRO_PATH/$i/archiving.png" \
    "$REPRO_PATH/$i/reproducing-custom.png" "$i"
done