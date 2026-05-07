package com.aiimage.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FolderDto(
    Long id,
    String name,
    Long parentId,
    String description,
    Integer imageCount,
    List<FolderDto> children,
    LocalDateTime createdAt
) {}
