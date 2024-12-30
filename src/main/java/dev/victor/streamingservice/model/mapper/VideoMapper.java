package dev.victor.streamingservice.model.mapper;

import dev.victor.streamingservice.model.dto.request.CreateVideoInfo;
import dev.victor.streamingservice.model.entity.Video;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Video fromCreateVideoInfo(CreateVideoInfo createVideoInfo);
}
