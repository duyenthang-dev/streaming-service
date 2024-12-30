package dev.victor.streamingservice.model.entity;

import dev.victor.streamingservice.model.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "video")
@Getter
@Setter
public class Video extends BaseEntity {
    private String title;
    private String description;
    private String url;

    @Column(name="content_type")
    private String contentType;

    @Column(name = "material_id")
    private String materialId;
}
