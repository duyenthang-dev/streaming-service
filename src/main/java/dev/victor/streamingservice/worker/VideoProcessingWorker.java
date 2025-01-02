package dev.victor.streamingservice.worker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.victor.streamingservice.model.entity.Video;
import dev.victor.streamingservice.model.enums.VideoConversionStatus;
import dev.victor.streamingservice.repository.VideoRepository;

public class VideoProcessingWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingWorker.class);

    private final VideoRepository videoRepository;    
    private File originalFile;
    private String outputFolder;
    private String id;

    public VideoProcessingWorker(File originalFile, String outputFolder, VideoRepository videoRepository, String id) {
        this.originalFile = originalFile;
        this.outputFolder = outputFolder;
        this.videoRepository = videoRepository;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            // create folder if not exitst
            File outputDir = new File(outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            Video video = videoRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new RuntimeException("Video not found"));
            video.setConversionStatus(VideoConversionStatus.PROCESSING);
            videoRepository.save(video);

            transcodeToM3u8(originalFile, new File(outputFolder));
        } catch (IOException e) {
            logger.error("Error during video transcoding: " + e.getMessage());
        }
    }

    public void transcodeToM3u8(File source, File outputDir) throws RuntimeException, IOException {
        logger.info("Start transcoding video to m3u8 format");
        // update status of video to PROCESSING
        
        String sourcePath = source.getAbsolutePath();
        var command = buildCommand(sourcePath, outputDir.getAbsolutePath());
        Process process = new ProcessBuilder().command(command).directory(outputDir).start();
        new Thread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logger.info(line);
                }
            } catch (IOException e) {
                logger.error("Error reading process input stream: " + e.getMessage());
            }
        }).start();

        new Thread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logger.info(line);
                }
            } catch (IOException e) {
                logger.error("Error reading process error stream: " + e.getMessage());
            }
        }).start();

        try {
            if (process.waitFor() != 0) {
                throw new RuntimeException("Process exited with " + process.exitValue());
            }
            logger.info("Video transcoding completed successfully");
            videoRepository.findById(UUID.fromString(id))
                    .ifPresent(video -> {
                        video.setConversionStatus(VideoConversionStatus.COMPLETED);
                        videoRepository.save(video);
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> buildLowQualityCommand(String sourcePath, String outputDirPath) {
        return List.of(
                "ffmpeg", "-i", sourcePath,
                "-vf", "scale=640:360,setsar=1",
                "-c:a", "aac", "-c:v", "libx264",
                "-b:v", "800k", "-b:a", "128k",
                "-hls_time", "20", "-hls_list_size", "0",
                "-maxrate", "1M", "-bufsize", "2M",
                "-hls_segment_filename", outputDirPath + "/%03d.ts",
                String.format("%s/index.m3u8", outputDirPath));
    }

    private List<String> buildCommand(String sourcePath, String outputDirPath) {
        return List.of(
                "ffmpeg", "-i", sourcePath,
                "-vf", "scale=1920:1080,setsar=1",
                "-c:a", "aac", "-c:v", "libx264",
                "-b:v", "5000k", "-b:a", "192k",
                "-hls_time", "20", "-hls_list_size", "0",
                "-maxrate", "2M", "-bufsize", "2M",
                "-hls_segment_filename", outputDirPath + "/%03d.ts",
                String.format("%s/index.m3u8", outputDirPath));
    }
}
