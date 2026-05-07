package com.aiimage.controller;

import com.aiimage.dto.*;
import com.aiimage.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Image CRUD, search, upload, scan, and batch operations")
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    @Operation(summary = "Search images", description = "Search images with keyword, folder, favorite, tag filters and pagination")
    public ResponseEntity<PageResponse<ImageDto>> searchImages(
            @RequestParam(required = false) @Parameter(description = "Search keyword (matches prompt, model, filename)") String keyword,
            @RequestParam(required = false) @Parameter(description = "Filter by folder ID") Long folderId,
            @RequestParam(required = false) @Parameter(description = "Filter favorites only") Boolean favorite,
            @RequestParam(required = false) @Parameter(description = "Filter by tag IDs") List<Long> tagIds,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "48") @Parameter(description = "Page size") int size,
            @RequestParam(defaultValue = "createdAt,desc") @Parameter(description = "Sort field and direction, e.g. createdAt,desc") String sort) {
        return ResponseEntity.ok(imageService.searchImages(keyword, folderId, favorite, tagIds, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get image detail")
    public ResponseEntity<ImageDto> getImage(@PathVariable @Parameter(description = "Image ID") Long id) {
        return ResponseEntity.ok(imageService.getImage(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload images", description = "Upload one or more image files with optional per-file metadata")
    public ResponseEntity<List<ImageUploadResponse>> uploadImages(
            @RequestParam("files") @Parameter(description = "Image files to upload") List<MultipartFile> files,
            @RequestParam(required = false) @Parameter(description = "Generation prompt") String prompt,
            @RequestParam(required = false) @Parameter(description = "Model name") String model,
            @RequestParam(required = false) @Parameter(description = "Sampling steps") Integer steps,
            @RequestParam(required = false) @Parameter(description = "CFG scale") Double cfgScale,
            @RequestParam(required = false) @Parameter(description = "Random seed") Long seed,
            @RequestParam(required = false) @Parameter(description = "Sampler name") String sampler,
            @RequestParam(required = false) @Parameter(description = "Per-file metadata JSON array") String metadataList) {
        return ResponseEntity.ok(imageService.uploadImages(files, prompt, model, steps, cfgScale, seed, sampler, metadataList));
    }

    @PostMapping(value = "/preview-metadata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview metadata", description = "Extract metadata from files without saving to storage or DB")
    public ResponseEntity<List<ImagePreviewResponse>> previewMetadata(
            @RequestParam("files") @Parameter(description = "Image files to analyze") List<MultipartFile> files) {
        return ResponseEntity.ok(imageService.previewMetadata(files));
    }

    @PostMapping("/scan")
    @Operation(summary = "Scan directory", description = "Scan a local directory and import images with auto-metadata extraction")
    public ResponseEntity<List<ImageUploadResponse>> scanDirectory(@RequestBody ScanRequest request) {
        return ResponseEntity.ok(imageService.scanDirectory(request.directoryPath()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete image")
    public ResponseEntity<Void> deleteImage(@PathVariable @Parameter(description = "Image ID") Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Batch delete images")
    public ResponseEntity<Void> batchDelete(@RequestBody @Parameter(description = "List of image IDs") List<Long> ids) {
        imageService.batchDelete(ids);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite")
    public ResponseEntity<ImageDto> toggleFavorite(@PathVariable @Parameter(description = "Image ID") Long id) {
        return ResponseEntity.ok(imageService.toggleFavorite(id));
    }

    @PutMapping("/{id}/metadata")
    @Operation(summary = "Update image metadata", description = "Update generation metadata (prompt, model, steps, etc.)")
    public ResponseEntity<ImageDto> updateMetadata(@PathVariable @Parameter(description = "Image ID") Long id, @RequestBody UpdateMetadataRequest request) {
        return ResponseEntity.ok(imageService.updateMetadata(id, request));
    }

    @PutMapping("/{id}/tags")
    @Operation(summary = "Set image tags", description = "Replace all tags on an image with the given tag IDs")
    public ResponseEntity<ImageDto> setTags(@PathVariable @Parameter(description = "Image ID") Long id, @RequestBody @Parameter(description = "Tag IDs to assign") List<Long> tagIds) {
        return ResponseEntity.ok(imageService.setTags(id, tagIds));
    }

    @PostMapping("/batch/tags")
    @Operation(summary = "Batch tag images", description = "Add tags to multiple images at once")
    public ResponseEntity<Void> batchTag(@Valid @RequestBody BatchTagRequest request) {
        imageService.batchTag(request.imageIds(), request.tagIds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/tags")
    @Operation(summary = "Append tags to image", description = "Add tags to an image without removing existing ones")
    public ResponseEntity<ImageDto> appendTags(
            @PathVariable @Parameter(description = "Image ID") Long id,
            @RequestBody @Parameter(description = "Tag IDs to add") List<Long> tagIds) {
        return ResponseEntity.ok(imageService.appendTags(id, tagIds));
    }

    @DeleteMapping("/{id}/tags")
    @Operation(summary = "Remove tags from image", description = "Remove specific tags from an image")
    public ResponseEntity<ImageDto> removeTagsFromImage(
            @PathVariable @Parameter(description = "Image ID") Long id,
            @RequestBody @Parameter(description = "Tag IDs to remove") List<Long> tagIds) {
        return ResponseEntity.ok(imageService.removeTagsFromImage(id, tagIds));
    }

    @DeleteMapping("/batch/tags")
    @Operation(summary = "Batch remove tags", description = "Remove specified tags from multiple images at once")
    public ResponseEntity<Void> batchRemoveTags(@Valid @RequestBody BatchRemoveTagRequest request) {
        imageService.batchRemoveTags(request.imageIds(), request.tagIds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download image", description = "Download the original image file with complete metadata")
    public ResponseEntity<Resource> downloadImage(@PathVariable @Parameter(description = "Image ID") Long id) throws IOException {
        ImageService.ImageFileInfo fileInfo = imageService.getImageFile(id);
        java.nio.file.Path filePath = fileInfo.filePath();
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        InputStream is = new FileInputStream(filePath.toFile());
        String filename = URLEncoder.encode(fileInfo.originalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/{id}/workflow")
    @Operation(summary = "Download workflow JSON", description = "Download the ComfyUI workflow JSON for an image")
    public ResponseEntity<Resource> downloadWorkflow(@PathVariable @Parameter(description = "Image ID") Long id) {
        String workflowJson = imageService.getWorkflowJsonString(id);
        if (workflowJson == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = workflowJson.getBytes(StandardCharsets.UTF_8);
        String filename = "workflow_" + id + ".json";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new InputStreamResource(new java.io.ByteArrayInputStream(bytes)));
    }
}
