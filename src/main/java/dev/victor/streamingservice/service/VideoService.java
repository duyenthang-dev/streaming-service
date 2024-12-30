package dev.victor.streamingservice.service;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.dto.response.UploadVideoResponse;
import dev.victor.streamingservice.model.entity.Video;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

public interface VideoService {
    Video createVideo(CreateVideoInfo request);
    UploadVideoResponse saveVideo(MultipartFile file, String id);
    UploadVideoResponse uploadVideo(HttpServletRequest request ) throws IOException, FileUploadException;
}
