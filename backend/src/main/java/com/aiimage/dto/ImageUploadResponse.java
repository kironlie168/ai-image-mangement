package com.aiimage.dto;

import java.util.List;

public record ImageUploadResponse(Long id, String originalFilename, String status, String errorMessage) {
    public static ImageUploadResponse success(Long id, String filename) {
        return new ImageUploadResponse(id, filename, "success", null);
    }

    public static ImageUploadResponse error(String filename, String error) {
        return new ImageUploadResponse(null, filename, "error", error);
    }
}
