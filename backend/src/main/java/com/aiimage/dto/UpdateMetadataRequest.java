package com.aiimage.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMetadataRequest(
    String prompt,
    String negativePrompt,
    String model,
    Integer steps,
    Double cfgScale,
    Long seed,
    String sampler
) {}
