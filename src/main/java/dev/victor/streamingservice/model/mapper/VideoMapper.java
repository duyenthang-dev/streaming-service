package dev.victor.streamingservice.model.mapper;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.entity.Video;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "conversionStatus", expression = "java(dev.victor.streamingservice.model.enums.VideoConversionStatus.PENDING)")
    Video fromCreateVideoInfo(CreateVideoInfo createVideoInfo);
}
