package com.aiimage.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ai_image", indexes = {
    @Index(name = "idx_model_created", columnList = "model,createdAt"),
    @Index(name = "idx_is_favorite", columnList = "isFavorite"),
    @Index(name = "idx_folder_id", columnList = "folderId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String originalFilename;

    @Column(nullable = false, length = 100, unique = true)
    private String storedFilename;

    @Column(nullable = false, length = 1000)
    private String filePath;

    @Column(length = 1000)
    private String thumbnailPath;

    private Long fileSize;
    private Integer width;
    private Integer height;

    @Column(length = 10)
    private String format;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    private String negativePrompt;

    @Column(length = 255)
    private String model;

    private Integer steps;

    @Column(name = "cfg_scale")
    private Double cfgScale;

    private Long seed;

    @Column(length = 100)
    private String sampler;

    @Column(name = "workflow_json", columnDefinition = "MEDIUMTEXT")
    private String workflowJson;

    @Column(name = "comfyui_prompt_json", columnDefinition = "MEDIUMTEXT")
    private String comfyuiPromptJson;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    @Column(name = "folder_id", insertable = false, updatable = false)
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToMany
    @JoinTable(name = "image_tag",
        joinColumns = @JoinColumn(name = "image_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
