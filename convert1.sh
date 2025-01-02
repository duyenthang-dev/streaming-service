#!/bin/bash

videoId=""
res=720

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -videoId=*) videoId="${1#*=}"; shift ;;
        -res=*) res="${1#*=}"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
done
videoPath="E:/backend/Java/streaming-service/videos"
hlsOutputPath="${videoPath}/hls_${videoId}"

start_time=$(date +%s)
# Create output directories for each resolution
mkdir -p "$hlsOutputPath/v0" "$hlsOutputPath/v1" "$hlsOutputPath/v2"


ffmpeg -i "${videoPath}/${videoId}.mp4" \
  -filter_complex \
    "[0:v]split=3[v1][v2][v3]; \
     [v1]scale=w=1920:h=1080[v1out]; \
     [v2]scale=w=1280:h=720[v2out]; \
     [v3]scale=w=854:h=480[v3out]" \
  -map "[v1out]" -c:v:0 libx264 -b:v:0 5000k -maxrate:v:0 5350k -bufsize:v:0 7500k \
  -map "[v2out]" -c:v:1 libx264 -b:v:1 2800k -maxrate:v:1 2996k -bufsize:v:1 4200k \
  -map "[v3out]" -c:v:2 libx264 -b:v:2 1400k -maxrate:v:2 1498k -bufsize:v:2 2100k \
  -map a:0 -c:a aac -b:a:0 192k -ac 2 \
  -map a:0 -c:a aac -b:a:1 128k -ac 2 \
  -map a:0 -c:a aac -b:a:2 96k -ac 2 \
  -f hls \
  -hls_time 20 \
  -hls_playlist_type vod \
  -hls_flags independent_segments \
  -hls_segment_type mpegts \
  -hls_segment_filename stream_%v/data%03d.ts \
  -master_pl_name master.m3u8 \
  -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2" \
  stream_%v/playlist.m3u8

# Create a master playlist file (initially empty)
# MASTER_PLAYLIST="$hlsOutputPath/master.m3u8"
# cat > "$MASTER_PLAYLIST" <<EOL
# #EXTM3U
# EOL

# Step 1: Encode 960x540 (lowest resolution, prioritized)
# echo "Starting 850x480 encoding..."
# ffmpeg -i "${videoPath}/${videoId}.mp4" \
# -vf "scale=w=850:h=480:force_original_aspect_ratio=decrease" \
# -c:v libx264 -preset fast -profile:v baseline -crf 20 -sc_threshold 0 -g 48 -keyint_min 48 \
# -b:v 1400k  -maxrate 1498k  -bufsize 2100k  \
# -c:a aac -b:a 128k \
# -hls_time 20 \
# -hls_list_size 0 \
# -hls_segment_filename "$hlsOutputPath/v0/segment%d.ts" \
# "$hlsOutputPath/v0/playlist.m3u8" && \
# echo "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=960x540
# v0/playlist.m3u8" >> "$MASTER_PLAYLIST" &

# # Step 2: Encode 1280x720 and 1920x1080 simultaneously
# echo "Starting 1280x720 and 1920x1080 encoding..."

# ffmpeg -i "${videoPath}/${videoId}.mp4" \
# -vf "scale=w=1280:h=720:force_original_aspect_ratio=decrease" \
# -c:v libx264 -preset fast -profile:v main -crf 20 -sc_threshold 0 -g 48 -keyint_min 48 \
# -b:v 2800k -maxrate 2996k -bufsize 4200k \
# -c:a aac -b:a 128k \
# -hls_time 20 \
# -hls_list_size 0 \
# -hls_segment_filename "$hlsOutputPath/v1/segment%d.ts" \
# "$hlsOutputPath/v1/playlist.m3u8" && \
# echo "#EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=1280x720
# v1/playlist.m3u8" >> "$MASTER_PLAYLIST" &

# ffmpeg -i "${videoPath}/${videoId}.mp4" \
# -vf "scale=w=1920:h=1080:force_original_aspect_ratio=decrease" \
# -c:v libx264 -profile:v main -crf 20 -sc_threshold 0 -g 48 -keyint_min 48 \
# -b:v 5000k  -maxrate 5350k -bufsize 7500k \
# -c:a aac -b:a 192k \
# -hls_time 20 \
# -hls_list_size 0 \
# -hls_segment_filename "$hlsOutputPath/v2/segment%d.ts" \
# "$hlsOutputPath/v2/playlist.m3u8" && \
# echo "#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1920x1080
# v2/playlist.m3u8" >> "$MASTER_PLAYLIST" &

# # # Wait for all background processes to finish
# wait
end_time=$(date +%s)
echo "Encoding completed in $((end_time - start_time)) seconds."

echo "All HLS files and master playlist generated successfully in $hlsOutputPath."
