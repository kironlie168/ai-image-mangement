package com.aiimage.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(
        @NotBlank(message = "Tag name is required")
        String name,
        String color
) {}
