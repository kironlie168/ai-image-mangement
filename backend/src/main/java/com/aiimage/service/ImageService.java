package com.aiimage.service;

import com.aiimage.dto.*;
import com.aiimage.entity.AiImage;
import com.aiimage.entity.Tag;
import com.aiimage.exception.ResourceNotFoundException;
import com.aiimage.repository.AiImageRepository;
import com.aiimage.repository.TagRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final AiImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final FileStorageService fileStorageService;
    private final MetadataExtractorService metadataExtractor;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.storage.images-dir}")
    private String imagesDir;

    @Value("${app.storage.thumbnails-dir}")
    private String thumbnailsDir;

    public PageResponse<ImageDto> searchImages(String keyword, Long folderId, Boolean favorite,
                                                List<Long> tagIds, int page, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                sorting = Sort.by(parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, parts[0]);
            }
        }
        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<AiImage> imagePage = imageRepository.searchImages(keyword, folderId, favorite, tagIds, pageable);
        List<ImageDto> dtos = imagePage.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(dtos, imagePage.getNumber(), imagePage.getSize(),
                imagePage.getTotalElements(), imagePage.getTotalPages());
    }

    public ImageDto getImage(Long id) {
        AiImage image = imageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.image(id));
        return toDto(image);
    }

    @Transactional
    public List<ImageUploadResponse> uploadImages(List<MultipartFile> files, String prompt, String model,
                                                   Integer steps, Double cfgScale, Long seed, String sampler,
                                                   String metadataListJson) {
        List<Map<String, String>> perFileMetadata = parseMetadataListJson(metadataListJson);
        List<ImageUploadResponse> results = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                String storedName = fileStorageService.storeFile(file);
                AiImage image = buildImage(file, storedName, prompt, model, steps, cfgScale, seed, sampler);

                // Apply per-file metadata if provided (user-edited preview values)
                if (perFileMetadata != null && i < perFileMetadata.size()) {
                    applyMetadata(image, perFileMetadata.get(i));
                }

                // Auto-extract metadata (fills remaining null fields)
                var metadata = metadataExtractor.extractMetadata(
                        fileStorageService.getImagePath(storedName), image.getFormat());
                applyMetadata(image, metadata);

                image = imageRepository.save(image);
                results.add(ImageUploadResponse.success(image.getId(), image.getOriginalFilename()));
            } catch (Exception e) {
                results.add(ImageUploadResponse.error(file.getOriginalFilename(), e.getMessage()));
            }
        }
        return results;
    }

    private List<Map<String, String>> parseMetadataListJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public List<ImageUploadResponse> scanDirectory(String directoryPath) {
        List<Path> files = fileStorageService.scanDirectory(directoryPath);
        List<ImageUploadResponse> results = new ArrayList<>();
        for (Path file : files) {
            try {
                String storedName = fileStorageService.storeFile(file);
                String originalName = file.getFileName().toString();

                AiImage image = AiImage.builder()
                        .originalFilename(originalName)
                        .storedFilename(storedName)
                        .filePath(imagesDir + "/" + storedName)
                        .thumbnailPath(thumbnailsDir + "/" + storedName)
                        .format(getExtension(originalName))
                        .build();

                // Read dimensions
                try {
                    BufferedImage bi = fileStorageService.readImageDimensions(storedName);
                    image.setWidth(bi.getWidth());
                    image.setHeight(bi.getHeight());
                    image.setFileSize(java.nio.file.Files.size(fileStorageService.getImagePath(storedName)));
                } catch (IOException ignored) {}

                // Try metadata extraction
                var metadata = metadataExtractor.extractMetadata(
                        fileStorageService.getImagePath(storedName), image.getFormat());
                applyMetadata(image, metadata);

                image = imageRepository.save(image);
                results.add(ImageUploadResponse.success(image.getId(), originalName));
            } catch (Exception e) {
                results.add(ImageUploadResponse.error(file.getFileName().toString(), e.getMessage()));
            }
        }
        return results;
    }

    public List<ImagePreviewResponse> previewMetadata(List<MultipartFile> files) {
        List<ImagePreviewResponse> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String format = getExtension(file.getOriginalFilename());
                Map<String, String> metadata = new HashMap<>();
                Integer width = null, height = null;

                // Read metadata from InputStream without saving to disk
                try (InputStream is = file.getInputStream()) {
                    metadata = metadataExtractor.extractMetadata(is, format);
                }

                // Read dimensions from a fresh stream
                try (InputStream is = file.getInputStream()) {
                    BufferedImage bi = ImageIO.read(is);
                    if (bi != null) {
                        width = bi.getWidth();
                        height = bi.getHeight();
                    }
                } catch (IOException ignored) {}

                results.add(new ImagePreviewResponse(
                    file.getOriginalFilename(),
                    file.getSize(),
                    width, height, format,
                    metadata.get("prompt"),
                    metadata.get("negativePrompt"),
                    metadata.get("model"),
                    metadata.containsKey("steps") ? Integer.parseInt(metadata.get("steps")) : null,
                    metadata.containsKey("cfgScale") ? Double.parseDouble(metadata.get("cfgScale")) : null,
                    metadata.containsKey("seed") ? Long.parseLong(metadata.get("seed")) : null,
                    metadata.get("sampler"),
                    metadata.get("workflowJson"),
                    metadata.get("comfyuiPromptJson")
                ));
            } catch (Exception e) {
                results.add(new ImagePreviewResponse(
                    file.getOriginalFilename(), file.getSize(),
                    null, null, getExtension(file.getOriginalFilename()),
                    null, null, null, null, null, null, null,
                    null, null
                ));
            }
        }
        return results;
    }

    @Transactional
    public void deleteImage(Long id) {
        AiImage image = imageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.image(id));
        fileStorageService.deleteFile(image.getStoredFilename());
        imageRepository.delete(image);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            deleteImage(id);
        }
    }

    @Transactional
    public ImageDto toggleFavorite(Long id) {
        AiImage image = imageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.image(id));
        image.setIsFavorite(!image.getIsFavorite());
        image = imageRepository.save(image);
        return toDto(image);
    }

    @Transactional
    public ImageDto updateMetadata(Long id, UpdateMetadataRequest req) {
        AiImage image = imageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.image(id));
        if (req.prompt() != null) image.setPrompt(req.prompt());
        if (req.negativePrompt() != null) image.setNegativePrompt(req.negativePrompt());
        if (req.model() != null) image.setModel(req.model());
        if (req.steps() != null) image.setSteps(req.steps());
        if (req.cfgScale() != null) image.setCfgScale(req.cfgScale());
        if (req.seed() != null) image.setSeed(req.seed());
        if (req.sampler() != null) image.setSampler(req.sampler());
        image = imageRepository.save(image);
        return toDto(image);
    }

    @Transactional
    public ImageDto setTags(Long imageId, List<Long> tagIds) {
        AiImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.image(imageId));

        if (tagIds.isEmpty()) {
            image.setTags(new HashSet<>());
            image = imageRepository.save(image);
            return toDto(image);
        }

        List<Tag> foundTags = tagRepository.findAllById(tagIds);
        if (foundTags.size() != tagIds.size()) {
            Set<Long> found = foundTags.stream().map(Tag::getId).collect(java.util.stream.Collectors.toSet());
            List<Long> missing = tagIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Tags not found: " + missing);
        }

        image.setTags(new HashSet<>(foundTags));
        image = imageRepository.save(image);
        return toDto(image);
    }

    @Transactional
    public void batchTag(List<Long> imageIds, List<Long> tagIds) {
        if (imageIds.isEmpty() || tagIds.isEmpty()) {
            throw new IllegalArgumentException("imageIds and tagIds must not be empty");
        }

        // Validate all image IDs exist
        long existingImageCount = imageRepository.countByIdIn(imageIds);
        if (existingImageCount != imageIds.size()) {
            List<Long> foundIds = imageRepository.findAllById(imageIds).stream().map(AiImage::getId).toList();
            Set<Long> found = new HashSet<>(foundIds);
            List<Long> missing = imageIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Images not found: " + missing);
        }

        // Validate all tag IDs exist
        long existingTagCount = tagRepository.countByIdIn(tagIds);
        if (existingTagCount != tagIds.size()) {
            List<Long> foundIds = tagRepository.findAllById(tagIds).stream().map(Tag::getId).toList();
            Set<Long> found = new HashSet<>(foundIds);
            List<Long> missing = tagIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Tags not found: " + missing);
        }

        // Bulk insert with INSERT IGNORE to handle duplicates
        String sql = "INSERT IGNORE INTO image_tag (image_id, tag_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                int imageIdx = i / tagIds.size();
                int tagIdx = i % tagIds.size();
                ps.setLong(1, imageIds.get(imageIdx));
                ps.setLong(2, tagIds.get(tagIdx));
            }

            @Override
            public int getBatchSize() {
                return imageIds.size() * tagIds.size();
            }
        });
    }

    @Transactional
    public ImageDto appendTags(Long imageId, List<Long> tagIds) {
        AiImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.image(imageId));

        if (tagIds.isEmpty()) return toDto(image);

        List<Tag> foundTags = tagRepository.findAllById(tagIds);
        if (foundTags.size() != tagIds.size()) {
            Set<Long> found = foundTags.stream().map(Tag::getId).collect(java.util.stream.Collectors.toSet());
            List<Long> missing = tagIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Tags not found: " + missing);
        }

        image.getTags().addAll(new HashSet<>(foundTags));
        image = imageRepository.save(image);
        return toDto(image);
    }

    @Transactional
    public ImageDto removeTagsFromImage(Long imageId, List<Long> tagIds) {
        imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.image(imageId));

        imageRepository.removeTagsFromImage(imageId, tagIds);
        return getImage(imageId);
    }

    @Transactional
    public void batchRemoveTags(List<Long> imageIds, List<Long> tagIds) {
        if (imageIds.isEmpty() || tagIds.isEmpty()) {
            throw new IllegalArgumentException("imageIds and tagIds must not be empty");
        }

        int deleted = imageRepository.batchRemoveTags(imageIds, tagIds);
        if (deleted == 0) {
            // Not an error; the combination simply didn't exist
        }
    }

    @Transactional
    public ImageDto moveToFolder(Long imageId, Long folderId) {
        AiImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.image(imageId));
        if (folderId == null) {
            image.setFolder(null);
        } else {
            var folderRef = imageRepository.getReferenceById(folderId);
            // folderId FK check would happen at flush time
            image.setFolder(null); // reset then set via entity manager
        }
        image = imageRepository.save(image);
        return toDto(image);
    }

    /**
     * Returns the storage path and original filename for an image, for download purposes.
     */
    public ImageFileInfo getImageFile(Long id) {
        AiImage image = imageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.image(id));
        Path filePath = fileStorageService.getImagePath(image.getStoredFilename());
        return new ImageFileInfo(filePath, image.getOriginalFilename());
    }

    /**
     * Returns the raw workflow JSON for an image, or null if none exists.
     */
    public String getWorkflowJsonString(Long id) {
        AiImage image = imageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.image(id));
        return image.getWorkflowJson();
    }

    public record ImageFileInfo(Path filePath, String originalFilename) {}

    private AiImage buildImage(MultipartFile file, String storedName, String prompt, String model,
                                Integer steps, Double cfgScale, Long seed, String sampler) {
        String originalName = file.getOriginalFilename();
        AiImage image = AiImage.builder()
                .originalFilename(originalName != null ? originalName : "unknown")
                .storedFilename(storedName)
                .filePath(imagesDir + "/" + storedName)
                .thumbnailPath(thumbnailsDir + "/" + storedName)
                .fileSize(file.getSize())
                .format(getExtension(originalName))
                .prompt(prompt)
                .model(model)
                .steps(steps)
                .cfgScale(cfgScale)
                .seed(seed)
                .sampler(sampler)
                .build();

        try {
            BufferedImage bi = ImageIO.read(file.getInputStream());
            if (bi != null) {
                image.setWidth(bi.getWidth());
                image.setHeight(bi.getHeight());
            }
        } catch (IOException ignored) {}

        return image;
    }

    private void applyMetadata(AiImage image, Map<String, String> metadata) {
        if (metadata.containsKey("prompt") && image.getPrompt() == null)
            image.setPrompt(metadata.get("prompt"));
        if (metadata.containsKey("negativePrompt") && image.getNegativePrompt() == null)
            image.setNegativePrompt(metadata.get("negativePrompt"));
        if (metadata.containsKey("model") && image.getModel() == null)
            image.setModel(metadata.get("model"));
        if (metadata.containsKey("steps") && image.getSteps() == null)
            image.setSteps(Integer.parseInt(metadata.get("steps")));
        if (metadata.containsKey("cfgScale") && image.getCfgScale() == null)
            image.setCfgScale(Double.parseDouble(metadata.get("cfgScale")));
        if (metadata.containsKey("seed") && image.getSeed() == null)
            image.setSeed(Long.parseLong(metadata.get("seed")));
        if (metadata.containsKey("sampler") && image.getSampler() == null)
            image.setSampler(metadata.get("sampler"));
        if (metadata.containsKey("width") && image.getWidth() == null)
            image.setWidth(Integer.parseInt(metadata.get("width")));
        if (metadata.containsKey("height") && image.getHeight() == null)
            image.setHeight(Integer.parseInt(metadata.get("height")));
        // Always set workflow/prompt JSON (may overwrite on re-upload)
        if (metadata.containsKey("workflowJson"))
            image.setWorkflowJson(metadata.get("workflowJson"));
        if (metadata.containsKey("comfyuiPromptJson"))
            image.setComfyuiPromptJson(metadata.get("comfyuiPromptJson"));
    }

    private ImageDto toDto(AiImage image) {
        return new ImageDto(
                image.getId(),
                image.getOriginalFilename(),
                image.getThumbnailPath() != null ? "/files/" + image.getThumbnailPath() : null,
                image.getFilePath() != null ? "/files/" + image.getFilePath() : null,
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getFormat(),
                image.getPrompt(),
                image.getNegativePrompt(),
                image.getModel(),
                image.getSteps(),
                image.getCfgScale(),
                image.getSeed(),
                image.getSampler(),
                image.getWorkflowJson(),
                image.getComfyuiPromptJson(),
                image.getIsFavorite(),
                image.getFolder() != null ? image.getFolder().getId() : null,
                image.getFolder() != null ? image.getFolder().getName() : null,
                image.getTags().stream().map(t -> new TagDto(t.getId(), t.getName(), t.getColor())).toList(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
