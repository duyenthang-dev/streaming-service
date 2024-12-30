package dev.victor.streamingservice.model.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadVideoResponse {
    private boolean success;
    private String id;
    private String folderPath;
}
