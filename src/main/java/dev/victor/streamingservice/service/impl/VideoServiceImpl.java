package dev.victor.streamingservice.service.impl;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.dto.response.UploadVideoResponse;
import dev.victor.streamingservice.model.entity.Video;
import dev.victor.streamingservice.model.enums.VideoConversionStatus;
import dev.victor.streamingservice.model.mapper.VideoMapper;
import dev.victor.streamingservice.repository.VideoRepository;
import dev.victor.streamingservice.repository.specs.VideoSpecs;
import dev.victor.streamingservice.service.VideoService;
import dev.victor.streamingservice.utils.SortOrderUtil;
import dev.victor.streamingservice.worker.VideoProcessingWorker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final String videoPath = Paths.get("").toAbsolutePath().toString() + "/videos/";

    @Override
    public Video createVideo(CreateVideoInfo request) {
        var video = videoMapper.fromCreateVideoInfo(request);
        return videoRepository.save(video);
    }

    @Override
    public UploadVideoResponse saveVideo(MultipartFile file, String id) {
        // 1.
        return null;
    }

    @Override
    public UploadVideoResponse uploadVideo(HttpServletRequest request, String id)
            throws IOException, FileUploadException {
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(request);

        if (!isMultipart) {
            throw new RuntimeException("Request is not multipart");
        }
        JakartaServletFileUpload upload = new JakartaServletFileUpload();
        upload.getItemIterator(request).forEachRemaining(item -> {
            logger.info("Field name: {}", item.getFieldName());
            InputStream stream = item.getInputStream();
            String name = item.getFieldName();
            if (!item.isFormField()) {
                logger.info("File field {} with file name {} detected.", name, item.getName());
                String fileName = String.format("%s.mp4", id);
                Files.createDirectories(Paths.get(videoPath));
                OutputStream out = new FileOutputStream(videoPath + fileName);
                IOUtils.copy(stream, out);
                stream.close();
                out.close();
            }
        });

        var res = new UploadVideoResponse();
        res.setSuccess(true);
        res.setFolderPath("/videos/" + String.format("%s.mp4", id));
        res.setId(id);
        return res;
    }

    @Override
    public StreamingResponseBody m3u8Index(String videoId) {
        Video video = videoRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new RuntimeException("Video not found"));
        if (video.getConversionStatus().equals(VideoConversionStatus.PENDING)
                || video.getConversionStatus().equals(VideoConversionStatus.FAILED)) {
            throw new RuntimeException("Video is not ready yet");
        }

        Path path = Paths.get(String.format("%s/hls_%s/master.m3u8", videoPath, videoId));
        logger.info("Serving m3u8 index file for video with id: {}", videoId);
        if (!Files.exists(path)) {
            throw new RuntimeException("Video not found");
        }

        return outputStream -> {
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while streaming m3u8 index file", e);
            }
        };
    }

    

    @Override
    public StreamingResponseBody m3u8Segment(String videoId, String segment) {
        Video video = videoRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new RuntimeException("Video not found"));
        if (video.getConversionStatus().equals(VideoConversionStatus.PENDING)
                || video.getConversionStatus().equals(VideoConversionStatus.FAILED)) {
            throw new RuntimeException("Video is not ready yet");
        }

        Path path = Paths.get(String.format("%s/hls_%s/%s.ts", videoPath, videoId, segment));
        logger.info("Serving m3u8 segment file for video with id: {} and segment: {}", videoId, segment);
        if (!Files.exists(path)) {
            throw new RuntimeException("Video not found");
        }

        return outputStream -> {
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while streaming m3u8 segment file", e);
            }
        };
    }

    @Override
    public int convertVideo(String videoId) {
        Video video = videoRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new RuntimeException("Video not found"));
        if (video.getConversionStatus().equals(VideoConversionStatus.COMPLETED)
                || video.getConversionStatus().equals(VideoConversionStatus.PROCESSING)) {
            throw new RuntimeException("Video is already being processed");
        }

        // start transcoding worker
        File originalFile = new File(videoPath + videoId + ".mp4");
        String outputFolder = videoPath + "hls_" + videoId;
        VideoProcessingWorker worker = new VideoProcessingWorker(originalFile, outputFolder, videoRepository, videoId);
        Thread workerThread = new Thread(worker);
        workerThread.start();
        return 1;
    }

    @Override
    public Page<Video> getListVideo(int page, int size, String search, String conversionStatus, String sortBy) {
        var sortOrder = SortOrderUtil.getSortOrder(sortBy, false);
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(sortOrder));
        Specification<Video> filterSpecs = VideoSpecs.filterBy(search, conversionStatus);
        return videoRepository.findAll(filterSpecs, pageRequest);
    }

    @Override
    public StreamingResponseBody getResource(Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException("Video not found");
        }

        return outputStream -> {
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while streaming path", e);
            }
        };
    }
}
