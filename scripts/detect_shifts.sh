#!/bin/bash

# first argument: elements.txt (DOM element-screenshot coordinate
# correspondence) for archived page
ELEMENTS_TXT=$1
# second argument: h264-compressed mp4 file, created by ffmpeg.sh
VIDEO=$2

OUTPUT_PATH=/path/to/shifts
OUTPUT_FILE=detected_shifts

# string operation: delete longest match ending in "/" from beginning of $VIDEO
# (i.e.: remove path from $VIDEO)
OUTPUT="${VIDEO##*/}"
# string operation: delete shortest match starting with "." followed by
# arbitrary characters from end of $OUTPUT
# (i.e.: delete file extension, s.t. we end up with the bare video file name)
OUTPUT="${OUTPUT%.*}"

echo "Checking page $OUTPUT"

# Main class: de.webis.webarchive.ShiftDetector, writes detected element shifts
# into $OUTPUT.txt
#java -jar de-webis-shift-detector.jar "$ELEMENTS_TXT" "$VIDEO" > "$OUTPUT_PATH/$OUTPUT.txt"
java -jar de-webis-shift-detector.jar "$ELEMENTS_TXT" "$VIDEO" | sed "s/^/$OUTPUT,/" >> "$OUTPUT_PATH/$OUTPUT_FILE.csv"

#if [ -s "$OUTPUT_PATH/$OUTPUT.txt" ] # if $OUTPUT.txt exists and is not empty
if [ -s "$OUTPUT_PATH/$OUTPUT_FILE.csv" ] # if $OUTPUT.txt exists and is not empty
then
  echo "Page $OUTPUT probably has missing elements"
#else
#  rm "$OUTPUT_PATH/$OUTPUT.txt"
fi
