package com.aiimage.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImageDto(
    Long id,
    String originalFilename,
    String thumbnailUrl,
    String imageUrl,
    Long fileSize,
    Integer width,
    Integer height,
    String format,
    String prompt,
    String negativePrompt,
    String model,
    Integer steps,
    Double cfgScale,
    Long seed,
    String sampler,
    String workflowJson,
    String comfyuiPromptJson,
    Boolean isFavorite,
    Long folderId,
    String folderName,
    List<TagDto> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
