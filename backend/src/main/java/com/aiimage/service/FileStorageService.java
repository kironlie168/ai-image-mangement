package com.aiimage.service;

import com.aiimage.exception.StorageException;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    @Value("${app.storage.base-path}")
    private String basePath;

    @Value("${app.storage.images-dir}")
    private String imagesDir;

    @Value("${app.storage.thumbnails-dir}")
    private String thumbnailsDir;

    @Value("${app.storage.thumbnail-max-dim}")
    private int thumbnailMaxDim;

    @Value("${app.scan.allowed-extensions}")
    private String allowedExtensions;

    private Path originalsPath;
    private Path thumbnailsPath;
    private Set<String> allowedExtSet;

    @PostConstruct
    public void init() {
        originalsPath = Path.of(basePath, imagesDir);
        thumbnailsPath = Path.of(basePath, thumbnailsDir);
        try {
            Files.createDirectories(originalsPath);
            Files.createDirectories(thumbnailsPath);
        } catch (IOException e) {
            throw new StorageException("Could not create storage directories", e);
        }
        allowedExtSet = Set.of(allowedExtensions.toLowerCase().split(","));
    }

    public String storeFile(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String storedName = UUID.randomUUID() + ext;
        Path target = originalsPath.resolve(storedName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            generateThumbnail(storedName);
            return storedName;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + originalName, e);
        }
    }

    public String storeFile(Path sourcePath) {
        String originalName = sourcePath.getFileName().toString();
        String ext = "";
        if (originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String storedName = UUID.randomUUID() + ext;
        Path target = originalsPath.resolve(storedName);
        try {
            Files.copy(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
            generateThumbnail(storedName);
            return storedName;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + originalName, e);
        }
    }

    public void deleteFile(String storedFilename) {
        try {
            Files.deleteIfExists(originalsPath.resolve(storedFilename));
            Files.deleteIfExists(thumbnailsPath.resolve(storedFilename));
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + storedFilename, e);
        }
    }

    public Path getImagePath(String storedFilename) {
        return originalsPath.resolve(storedFilename);
    }

    public Path getThumbnailPath(String storedFilename) {
        return thumbnailsPath.resolve(storedFilename);
    }

    public BufferedImage readImageDimensions(String storedFilename) throws IOException {
        return ImageIO.read(getImagePath(storedFilename).toFile());
    }

    public List<Path> scanDirectory(String directoryPath) {
        Path dir = Path.of(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + directoryPath);
        }
        try (Stream<Path> walk = Files.walk(dir, 3)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return allowedExtSet.stream().anyMatch(name::endsWith);
                    })
                    .toList();
        } catch (IOException e) {
            throw new StorageException("Failed to scan directory: " + directoryPath, e);
        }
    }

    public record DirEntry(String name, String path, boolean isDirectory, long imageCount) {}

    public List<DirEntry> listDirectory(String directoryPath) {
        Path dir = Path.of(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + directoryPath);
        }
        List<DirEntry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted((a, b) -> {
                boolean aDir = Files.isDirectory(a);
                boolean bDir = Files.isDirectory(b);
                if (aDir != bDir) return aDir ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            }).forEach(p -> {
                String name = p.getFileName().toString();
                boolean isDir = Files.isDirectory(p);
                long imageCount = 0;
                if (isDir) {
                    try (Stream<Path> walk = Files.walk(p, 1)) {
                        imageCount = walk.filter(Files::isRegularFile)
                                .filter(f -> {
                                    String fn = f.getFileName().toString().toLowerCase();
                                    return allowedExtSet.stream().anyMatch(fn::endsWith);
                                })
                                .count();
                    } catch (IOException ignored) {}
                }
                entries.add(new DirEntry(name, p.toAbsolutePath().toString(), isDir, imageCount));
            });
        } catch (IOException e) {
            throw new StorageException("Failed to list directory: " + directoryPath, e);
        }
        return entries;
    }

    public record StorageStats(long usedBytes, long imageCount, long thumbnailCount) {}

    public StorageStats getStorageStats() {
        long usedBytes = 0;
        long imageCount = 0;
        try (Stream<Path> files = Files.list(originalsPath)) {
            List<Path> fileList = files.filter(Files::isRegularFile).toList();
            imageCount = fileList.size();
            usedBytes = fileList.stream().mapToLong(p -> {
                try { return Files.size(p); } catch (IOException e) { return 0; }
            }).sum();
        } catch (IOException ignored) {}

        long thumbnailCount = 0;
        try (Stream<Path> files = Files.list(thumbnailsPath)) {
            thumbnailCount = files.filter(Files::isRegularFile).count();
        } catch (IOException ignored) {}

        return new StorageStats(usedBytes, imageCount, thumbnailCount);
    }

    public List<String> getRootDirectories() {
        List<String> roots = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            roots.add(root.toString());
        }
        return roots;
    }

    private void generateThumbnail(String storedFilename) throws IOException {
        Path source = originalsPath.resolve(storedFilename);
        Path target = thumbnailsPath.resolve(storedFilename);
        Thumbnails.of(source.toFile())
                .size(thumbnailMaxDim, thumbnailMaxDim)
                .keepAspectRatio(true)
                .toFile(target.toFile());
    }
}
