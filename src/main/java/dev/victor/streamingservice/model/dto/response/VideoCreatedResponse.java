package dev.victor.streamingservice.model.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoCreatedResponse {
    private boolean success;
    private String id;
}
