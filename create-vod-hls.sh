#!/usr/bin/env bash
LC_NUMERIC="en_US.UTF-8"
START_TIME=$SECONDS
set -e

echo "-----START GENERATING HLS STREAM-----"
# Usage create-vod-hls.sh SOURCE_FILE [OUTPUT_NAME]
[[ ! "${1}" ]] && echo "Usage: create-vod-hls.sh SOURCE_FILE [OUTPUT_NAME]" && exit 1

# comment/add lines here to control which renditions would be created

# recommend resolution  bitrate  audio-rate
  # "426x240    400k    128k"
  # "640x360    800k     128k"
  # "842x480    1400k    192k"
  # "1280x720   2800k    192k"
  # "1920x1080  5000k    256k"
renditions=(
# resolution  bitrate  audio-rate
  "842x480    1400k    192k"
  "1280x720   2800k    192k"
)

segment_target_duration=10       # try to create a new segment every 10 seconds
max_bitrate_ratio=1.07          # maximum accepted bitrate fluctuations
rate_monitor_buffer_ratio=1.5   # maximum buffer size between bitrate conformance checks

#########################################################################

source="${1}"
target="${2}"
if [[ ! "${target}" ]]; then
  target="${source##*/}" # leave only last component of path
  target="${target%.*}"  # strip extension
fi
mkdir -p ${target}

sourceResolution="$(ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 ${source})"
arrIN=(${sourceResolution//x/ })
sourceWidth="${arrIN[0]}"
sourceHeight="${arrIN[1]}"

sourceAudioBitRate="$(ffprobe -v error -select_streams a:0 -show_entries stream=bit_rate -of csv=s=x:p=0 ${source})"
sourceAudioBitRateFormatted=$((sourceAudioBitRate / 1000))

fps=$(ffprobe ${source} 2>&1 | grep -oE '[[:digit:]]+(.[[:digit:]]+)? fps' | grep -oE '[[:digit:]]+(.[[:digit:]]+)?')
key_frames_interval=$(echo "$fps * 2" | awk '{printf "%.0f", $0}')
key_frames_interval=${key_frames_interval:-50}
key_frames_interval=$(( (key_frames_interval + 5) / 10 * 10 ))

static_params="-c:a aac -ar 48000 -c:v h264 -profile:v main -crf 19 -sc_threshold 0"
static_params+=" -g ${key_frames_interval} -keyint_min ${key_frames_interval} -hls_time ${segment_target_duration}"
static_params+=" -hls_playlist_type vod"

misc_params="-hide_banner -y"

master_playlist="#EXTM3U
#EXT-X-VERSION:3
"
cmd=""
resolutionValid=0
prevHeight=0
index=0

for rendition in "${renditions[@]}"; do
  rendition="${rendition/[[:space:]]+/ }"
  resolution="$(echo ${rendition} | cut -d ' ' -f 1)"
  bitrate="$(echo ${rendition} | cut -d ' ' -f 2)"
  audiorate="$(echo ${rendition} | cut -d ' ' -f 3)"
  
  audioBitRateFormatted=${audiorate%?}
  if [ $audioBitRateFormatted -gt $sourceAudioBitRateFormatted ]; then
      audiorate=${sourceAudioBitRateFormatted}k
  fi

  width="$(echo ${resolution} | grep -oE '^[[:digit:]]+')"
  height="$(echo ${resolution} | grep -oE '[[:digit:]]+$')"
  maxrate=$(echo "${bitrate} * ${max_bitrate_ratio}" | awk '{printf "%.0f", $0}')
  bufsize=$(echo "${bitrate} * ${rate_monitor_buffer_ratio}" | awk '{printf "%.0f", $0}')
  bandwidth="$(echo ${bitrate} | grep -oE '[[:digit:]]+')000"
  name="${height}p"
  
  if [ $sourceHeight -le $prevHeight ]; then
      echo "video source has height smaller than output height (${height})"
      break
  fi

  widthParam=0
  heightParam=0
  if [ $(((width / sourceWidth) * sourceHeight)) -gt $height ]; then
    widthParam=-2
    heightParam=$height
  else
    widthParam=$width
    heightParam=-2
  fi
  
  if [ $index -eq 0 ]; then
    echo "Generating lowest rendition (${name})..."
    ffmpeg ${misc_params} -i ${source} ${static_params} -vf scale=w=${widthParam}:h=${heightParam} \
      -b:v ${bitrate} -maxrate ${maxrate%.*}k -bufsize ${bufsize%.*}k -b:a ${audiorate} \
      -hls_segment_filename ${target}/${name}_%03d.ts ${target}/${name}.m3u8
    
    echo "Time taken for first rendition: $(($SECONDS - $START_TIME)) seconds"
    echo "Updating master playlist for lowest rendition..."
    master_playlist="#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=${bandwidth},RESOLUTION=${resolution}\n${name}.m3u8\n"
    echo -e "${master_playlist}" > ${target}/playlist.m3u8
  else
    cmd+=" ${static_params} -vf scale=w=${widthParam}:h=${heightParam}"
    cmd+=" -b:v ${bitrate} -maxrate ${maxrate%.*}k -bufsize ${bufsize%.*}k -b:a ${audiorate}"
    cmd+=" -hls_segment_filename ${target}/${name}_%03d.ts ${target}/${name}.m3u8"
    master_playlist+="#EXT-X-STREAM-INF:BANDWIDTH=${bandwidth},RESOLUTION=${resolution}\n${name}.m3u8\n"
  fi
  
  resolutionValid=1
  prevHeight=${height}
  index=$((index + 1))
done

if [ $resolutionValid -eq 1 ]; then
  echo "Generating remaining renditions..."
  ffmpeg ${misc_params} -i ${source} ${cmd}
  echo -e "${master_playlist}" > ${target}/playlist.m3u8
  echo "Done - encoded HLS is at ${target}/"
else
  echo "Video source is too small"
  exit 1
fi

ELAPSED_TIME=$(($SECONDS - $START_TIME))

echo "Elapsed time: ${ELAPSED_TIME}"
echo "-----FINISH GENERATING HLS STREAM-----"
