package com.aiimage.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchRemoveTagRequest(
        @NotEmpty(message = "imageIds must not be empty")
        List<Long> imageIds,
        @NotEmpty(message = "tagIds must not be empty")
        List<Long> tagIds
) {}
