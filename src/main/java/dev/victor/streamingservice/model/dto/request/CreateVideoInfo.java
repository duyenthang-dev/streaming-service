package dev.victor.streamingservice.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVideoInfo {
    private String title;
    private String description;
    private String contentType;
    private String materialId;
}
