package com.aiimage.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException image(Long id) {
        return new ResourceNotFoundException("Image not found: " + id);
    }

    public static ResourceNotFoundException tag(Long id) {
        return new ResourceNotFoundException("Tag not found: " + id);
    }

    public static ResourceNotFoundException folder(Long id) {
        return new ResourceNotFoundException("Folder not found: " + id);
    }
}
