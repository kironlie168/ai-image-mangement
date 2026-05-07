package com.aiimage.dto;

public record ImagePreviewResponse(
    String originalFilename,
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
    String comfyuiPromptJson
) {}
