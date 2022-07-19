#!/bin/bash

# first argument: screenshot of original web page at the time of archival
ORIGINAL_FRAME=$1
# second argument: screenshot of reproduced archive of web page
ARCHIVE_FRAME=$2
# string operation: remove everything up to the image's file extension
# (i.e. determine file extension)
EXT=${ORIGINAL_FRAME##*.}

OUTPUT_PATH=/path/to/pages

# dirname: remove filename from path
# basename: remove all but last folder from path
# result: page id (as folder name containing screenshot of original web page)
OUTPUT="$(basename "$(dirname "$ORIGINAL_FRAME")")"

echo "Converting page $OUTPUT"

# copy both frames to temporary directory
mkdir -p "$OUTPUT_PATH/tmp/$OUTPUT"
cp "$ORIGINAL_FRAME" "$OUTPUT_PATH/tmp/$OUTPUT/0.$EXT"
cp "$ARCHIVE_FRAME" "$OUTPUT_PATH/tmp/$OUTPUT/1.$EXT"

# Create directory for intermediary motion vector files
mkdir -p $OUTPUT_PATH/mv

# create two-frame h264 video
# parameters:
# -r 1: 1 fps
# -i .../%01d.$EXT: input all single-digit files with extension $EXT
#   sequentially
# -vcodec libx264: h264 encoding
# -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2": pad width and height to nearest multiple
#   of 2 (otherwise h264 complains)
# -me_method 4: full motion estimation (creates the most motion vectors)
#
## This is the video that serves as an input to the Java code.
#ffmpeg -r 1 -i "/tmp/$OUTPUT/%01d.$EXT" -vcodec libx264 \
#  -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2" -me_method 4 "mv/$OUTPUT.mp4"
#
# experiment 1:
# -crf 0: lossless compression
# -preset veryslow provides the best compression (see https://trac.ffmpeg.org/wiki/Encode/H.264)
#
#ffmpeg -r 1 -i "/tmp/$OUTPUT/%01d.$EXT" -vcodec libx264 \
#  -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2" -me_method 4 -preset veryslow -crf 0 "mv/$OUTPUT.mp4"
#

# always pad 1 row (column) if width or height are odd numbers or else
# ffmpeg will complain
# use bigger images width and height to fit the smaller+padding
# when archived image is larger than original -> use archived size and pad original
WIDTH="$(ffprobe -v error -show_entries stream=width -of default=noprint_wrappers=1 "$OUTPUT_PATH/tmp/$OUTPUT/0.$EXT" | grep -o '[0-9]\+')"
HEIGHT="$(ffprobe -v error -show_entries stream=height -of default=noprint_wrappers=1 "$OUTPUT_PATH/tmp/$OUTPUT/0.$EXT" | grep -o '[0-9]\+')"
HEIGHT_ARCHIVED="$(ffprobe -v error -show_entries stream=height -of default=noprint_wrappers=1 "$OUTPUT_PATH/tmp/$OUTPUT/1.$EXT" | grep -o '[0-9]\+')"
if [ $HEIGHT_ARCHIVED -gt $HEIGHT ]
then
  HEIGHT=$HEIGHT_ARCHIVED
fi
# [0:v] [1:v] concat=n=2:v=1 [v] is the concat filter:
#		n=2 is telling the filter that there are 2 input segments
#		v=1 is telling it that there will be one video stream per segment
#		The filter then concatenates these segments and produces one output
#		stream. [v] is the name for the output stream.
ffmpeg -r 1 -i "$OUTPUT_PATH/tmp/$OUTPUT/0.$EXT" -i "$OUTPUT_PATH/tmp/$OUTPUT/1.$EXT" -vcodec libx264 -filter_complex \
"[0:v]pad=ceil($WIDTH/2)*2:ceil($HEIGHT/2)*2[a];[1:v]pad=ceil($WIDTH/2)*2:ceil($HEIGHT/2)*2[b];[a][b]concat=n=2:v=1[out]" -map "[out]" -me_method 4 "$OUTPUT_PATH/mv/$OUTPUT.mp4"

# extract motion vectors
# parameters:
# -flags2 +export_mvs: export motion vectors into frame side-data (for later
#   extraction)
# -vf codecview=mv=pf+bf+bb: export ...
# ... pf: forward predicted motion vectors of P frames
# ... bf: forward predicted motion vectors of B frames
# ... bb: backward predicted motion vectors of B frames
#   (see: https://trac.ffmpeg.org/wiki/Debug/MacroblocksAndMotionVectors)
#
# The resulting video will show motion vectors as arrows in the second frame.
ffmpeg -flags2 +export_mvs -i "$OUTPUT_PATH/mv/$OUTPUT.mp4" -vf codecview=mv=pf+bf+bb \
  "$OUTPUT_PATH/tmp/$OUTPUT/$OUTPUT_mv.mp4"

# extract second frame of motion vector video, showing arrows, as png image
# parameters:
# -vf "select=gte(n\,1)": select frame n where n >= 1 (i.e. second frame)
# -vframes 1: output one frame
#
# This is just for debugging purposes, to look at the detected motion vectors.
ffmpeg -i "$OUTPUT_PATH/tmp/$OUTPUT/$OUTPUT_mv.mp4" -vf "select=gte(n\,1)" -vframes 1 \
  "$OUTPUT_PATH/mv/$OUTPUT.png"

# delete temporary folder
rm -r "$OUTPUT_PATH/tmp/$OUTPUT/"
