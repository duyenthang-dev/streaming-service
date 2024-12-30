package dev.victor.streamingservice.service.impl;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.dto.response.UploadVideoResponse;
import dev.victor.streamingservice.model.entity.Video;
import dev.victor.streamingservice.model.mapper.VideoMapper;
import dev.victor.streamingservice.repository.VideoRepository;
import dev.victor.streamingservice.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final String videoPath = Paths.get("").toAbsolutePath().toString() + "/videos/";;
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
    public UploadVideoResponse uploadVideo(HttpServletRequest request) throws IOException, FileUploadException {
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(request);
        if  (!isMultipart) {
            throw new RuntimeException("Request is not multipart");
        }
        JakartaServletFileUpload upload = new JakartaServletFileUpload();
        FileItemInputIterator iter = upload.getItemIterator(request);
        String fileName = null;
        String id = null;

        while (iter.hasNext()) {
            FileItemInput item = iter.next();
            if (item.isFormField() && "id".equals(item.getFieldName())) {
                id = IOUtils.toString(item.getInputStream(), "UTF-8");
                break;
            }
        }

        while (iter.hasNext()) {
            FileItemInput item = iter.next();
            String name = item.getFieldName();
            InputStream stream = item.getInputStream();
            if (!item.isFormField()){
                logger.info("File field {} with file name {} detected.", name, item.getName());
                fileName = String.format("%s_%s.mp4", id, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH")));
                Files.createDirectories(Paths.get(videoPath));
                OutputStream out = new FileOutputStream(videoPath + fileName);
                IOUtils.copy(stream, out);
                stream.close();
                out.close();
            }
        }
        var res = new UploadVideoResponse();
        res.setSuccess(true);
        res.setFolderPath("/videos/" + fileName);
        return res;
    }
}
