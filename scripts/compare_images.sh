#!/bin/bash

# first argument: original website's screenshot
ORIGINAL=$1
# second argument: archived website's screenshot
ARCHIVED=$2
# third argument: number of current page to control processing status
CURRENTPAGE=$3

OUTPUT_PATH=/path/to/webis-web-archive-17/pages/
# file to write output to
OUTPUT="compare_images/compared_images"

echo "Checking page $CURRENTPAGE"

PAGENUMBER="$(echo $CURRENTPAGE | sed 's/^[0]\{1,5\}//')"

# Main class: de.webis.webarchive.ImageComparator, writes results of comparison
# into $OUTPUT.csv and prepends pagenumber to every line
java -jar web-archive-image-comparator.jar "$ORIGINAL" "$ARCHIVED"  | sed "s/^/$PAGENUMBER,/" >> "$OUTPUT_PATH/$OUTPUT.csv"
