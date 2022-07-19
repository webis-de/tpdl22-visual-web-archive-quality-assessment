#!/bin/bash

# path to reproduced web pages
REPRO_PATH=/path/to/reproduced/pages/

# for all pages
for i in $(seq -f "%06g" 0 9999)
do
  # compare original and archived image
  ./reconstruct_images.sh "$REPRO_PATH/$i/script/elements.txt" "$REPRO_PATH/$i/archiving.png" \
    "$REPRO_PATH/mv/$i.mp4" "$REPRO_PATH/$i/reproducing-custom.png" "$i"
done