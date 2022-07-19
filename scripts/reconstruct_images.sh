#!/bin/bash

# first argument: elements.txt describing archived website
ELEMENTS=$1
# second argument: original website's screenshot
ORIGINAL=$2
# third argument: mp4 created with ffmpeg containing motion vectors of moved elements
VIDEO=$3
# fourth argument: archived website's screenshot
ARCHIVED=$4
# fifth argument: number of current page (to control processing status)
CURRENTPAGE=$5

OUTPUT_PATH=/path/to/pages/reconstructed_images
# file to write output to
OUTPUT=$CURRENTPAGE

echo "Checking page $CURRENTPAGE"

# Main class: de.webis.webarchive.ImageReconstructor, writes reconstructed images
# into $OUTPUT.csv
java -jar de-webis-image-reconstructor.jar "$ELEMENTS" "$ORIGINAL" "$VIDEO" "$ARCHIVED"
mv "out_original_sized.png" "$OUTPUT_PATH/$OUTPUT.png"
