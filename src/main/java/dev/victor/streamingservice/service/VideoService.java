package dev.victor.streamingservice.service;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.dto.response.UploadVideoResponse;
import dev.victor.streamingservice.model.entity.Video;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Path;

public interface VideoService {
    Video createVideo(CreateVideoInfo request);

    UploadVideoResponse saveVideo(MultipartFile file, String id);

    UploadVideoResponse uploadVideo(HttpServletRequest request, String id) throws IOException, FileUploadException;

    StreamingResponseBody getResource(Path path);

    StreamingResponseBody m3u8Index(String videoId);

    StreamingResponseBody m3u8Segment(String videoId, String segment);

    int convertVideo(String videoId);

    Page<Video> getListVideo(int page, int size, String search, String conversionStatus, String sortBy);

}
