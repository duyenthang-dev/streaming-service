#!/bin/bash
# get videoId from param when runscript: ./convert-scripts.sh -videoId="" -res=720
# Parse command line arguments
videoId=""
res=720

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -videoId=*) videoId="${1#*=}"; shift ;;
        -res=*) res="${1#*=}"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
done

if [ -z "$videoId" ]; then
    echo "Error: videoId is required"
    exit 1
fi

videoPath="E:/backend/Java/streaming-service/videos"
hlsOutputPath="${videoPath}/hls_${videoId}"
resolution=$res

echo "Converting video to HLS format..."
echo "Video path: ${videoPath}/${videoId}"
echo "HLS output path: ${hlsOutputPath}"

# Create output directory
mkdir -p $hlsOutputPath
# Start time
start_time=$(date +%s)

# Run ffmpeg command based on resolution
# if [ "$res" -eq 720 ]; then
#     ffmpeg -i "${videoPath}/${videoId}.mp4" \
#         -vf "scale=1280:720,setsar=1" \
#         -c:v libx264 -preset fast -b:v 1M \
#         -c:a aac -hls_list_size 0 -maxrate 665k -bufsize 1M \
#         -hls_time 20 \
#         -hls_segment_filename "${hlsOutputPath}/%03d.ts" "${hlsOutputPath}/mid.m3u8"
# elif [ "$res" -eq 1080 ]; then
#         ffmpeg -i "${videoPath}/${videoId}.mp4" \
#         -vf "scale=1920:1080,setsar=1" \
#         -c:a aac -c:v libx264 -b:v 2500k -b:a 128k -hls_time 20 \
#         -hls_list_size 0 -maxrate 2M -bufsize 2M \
#         -hls_segment_filename $hlsOutputPath/%03d.ts $hlsOutputPath/index.m3u8

# else
#     echo "Unsupported resolution: $res"
# fi
    

# ffmpeg -i "${videoPath}/${videoId}.mp4" \
#     -vf "scale=1920:1080,setsar=1" \
#     -c:a aac -c:v libx264 -b:v 2500k -b:a 128k -hls_time 20 \
#     -hls_list_size 0 -maxrate 2M -bufsize 2M \
#     -hls_segment_filename $hlsOutputPath/%03d.ts $hlsOutputPath/index.m3u8

ffmpeg -i "${videoPath}/${videoId}.mp4" \
-filter_complex \
"[0:v]split=3[v1][v2][v3]; \
 [v1]scale=w=842:h=480:force_original_aspect_ratio=decrease[v1out]; \
 [v2]scale=w=1280:h=720:force_original_aspect_ratio=decrease[v2out]; \
 [v3]scale=w=1920:h=1080:force_original_aspect_ratio=decrease[v3out]" \
-map "[v1out]" -map 0:a -c:v:0 libx264 -profile:v:0 main -crf 20 -sc_threshold 0 -g 48 -keyint_min 48 -b:v:0 1400k -maxrate:v:0 1498k -bufsize:v:0 2100k \
-map "[v2out]" -map 0:a -c:v:1 libx264 -profile:v:1 baseline -crf 20 -sc_threshold 0 -g 48 -keyint_min 48 -b:v:1 2800k -maxrate:v:1 2996k -bufsize:v:1 4200k \
-map "[v3out]" -map 0:a -c:v:2 libx264 -profile:v:2 main -crf 20 -sc_threshold 0 -g 48 -keyint_min 48 -b:v:2 5000k -maxrate:v:2 5350k  -bufsize:v:2 7500k  \
-c:a aac -b:a 128k \
-f hls \
-var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2" \
-master_pl_name master.m3u8 \
-hls_time 20 \
-hls_list_size 0 \
-hls_segment_filename "${hlsOutputPath}/v%v/segment%d.ts" \
"${hlsOutputPath}/v%v/playlist.m3u8"

end_time=$(date +%s)

if [ $? -eq 0 ]; then
    echo "Video conversion successful"
else
    echo "Video conversion failed"
fi

echo "Conversion completed in $(($end_time - $start_time)) seconds"