package dev.victor.streamingservice.controller;

import dev.victor.streamingservice.model.base.PageRespose;
import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.dto.response.ConvertVideoResponse;
import dev.victor.streamingservice.model.dto.response.UploadVideoResponse;
import dev.victor.streamingservice.model.dto.response.VideoCreatedResponse;
import dev.victor.streamingservice.service.VideoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/v1/videos")
@RequiredArgsConstructor
public class VideoController {
    private final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;
    private final String videoPath = Paths.get("").toAbsolutePath().toString() + "/videos/";

    @GetMapping
    public ResponseEntity<?> getListVideo(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "conversion_status", defaultValue = "COMPLETED") String conversionStatus,
            @RequestParam(value = "sort_by", defaultValue = "created_at desc") String sortBy) {
        logger.info("Getting list of videos, page: {}, size: {}", page, size);
        var videos = videoService.getListVideo(page, size, search, conversionStatus, sortBy);

        return ResponseEntity.ok(PageRespose.success(videos));
    }

    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(HttpServletRequest request, @PathVariable String id)
            throws FileUploadException, IOException, ServletException {

        logger.info("Uploading video in the streaming way");
        UploadVideoResponse res = videoService.uploadVideo(request, id);

        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<VideoCreatedResponse> createVideoInfo(@RequestBody CreateVideoInfo request) {
        logger.info("Creating video info with title: {}", request.getTitle());
        var video = videoService.createVideo(request);
        var response = new VideoCreatedResponse();
        response.setSuccess(true);
        response.setId(String.valueOf(video.getId()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/index.m3u8")
    public ResponseEntity<StreamingResponseBody> getHlsIndex(@PathVariable String id) {
        logger.info("Getting HLS index for video with id: {}", id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=index.m3u8");
        StreamingResponseBody body = videoService.m3u8Index(id);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/{resolution}/playlist.m3u8")
    public ResponseEntity<StreamingResponseBody> getHlsIndex(@PathVariable String id, @PathVariable String resolution) {
        logger.info("Getting HLS index for video with id: {}", id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=index.m3u8");
        Path path = Paths.get(String.format("%s/hls_%s/%s/playlist.m3u8", videoPath, id, resolution));
        StreamingResponseBody body = videoService.getResource(path);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }


    @GetMapping("/{id}/{segment}.ts")
    public ResponseEntity<StreamingResponseBody> getM3u8Segment(@PathVariable String id, @PathVariable String segment) {
        logger.info("Getting HLS segment for video with id: {} and segment: {}", id, segment);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=" + segment + ".ts");
        StreamingResponseBody body = videoService.m3u8Segment(id, segment);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/{resolution}/{segment}.ts")
    public ResponseEntity<StreamingResponseBody> getM3u8Segment(@PathVariable String id, @PathVariable String resolution, @PathVariable String segment) {
        logger.info("Getting HLS segment for video with id: {} and segment: {}", id, segment);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.apple.mpegurl");
        headers.set("Content-Disposition", "attachment;filename=" + segment + ".ts");
        Path path = Paths.get(String.format("%s/hls_%s/%s/%s.ts", videoPath, id, resolution, segment));
        StreamingResponseBody body = videoService.getResource(path);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<?> convertVideo(@PathVariable String id) {
        logger.info("Converting video with id: {}", id);
        var result = videoService.convertVideo(id);
        var res = new ConvertVideoResponse();
        if (result == 1) {
            res.setMessage("Video conversion started");
            res.setStatus("IN_PROGRESS");
            res.setVideoId(id);
            return ResponseEntity.ok(res);
        }
        res.setMessage("Video conversion failed");
        res.setStatus("FAILED");
        res.setVideoId(id);
        return ResponseEntity.ok(res);
    }
}
