package dev.victor.streamingservice.controller;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.dto.response.UploadVideoResponse;
import dev.victor.streamingservice.model.dto.response.VideoCreatedResponse;
import dev.victor.streamingservice.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/videos")
@RequiredArgsConstructor
public class VideoController {
    private final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(HttpServletRequest request) {

        logger.info("Uploading video");
        String id =  "abc";
        UploadVideoResponse res = null;
        try {
            res = videoService.uploadVideo(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
