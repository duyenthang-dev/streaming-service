package dev.victor.streamingservice.model.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConvertVideoResponse {
    private String videoId;
    private String status;
    private String message;
    
}
