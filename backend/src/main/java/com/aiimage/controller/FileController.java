package com.aiimage.controller;

import com.aiimage.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File system browsing and storage stats")
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/drives")
    @Operation(summary = "List root directories (drives on Windows)")
    public ResponseEntity<List<String>> getDrives() {
        return ResponseEntity.ok(fileStorageService.getRootDirectories());
    }

    @GetMapping("/browse")
    @Operation(summary = "List contents of a directory")
    public ResponseEntity<List<FileStorageService.DirEntry>> browse(
            @RequestParam @Parameter(description = "Directory path") String path) {
        return ResponseEntity.ok(fileStorageService.listDirectory(path));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get storage statistics")
    public ResponseEntity<FileStorageService.StorageStats> getStats() {
        return ResponseEntity.ok(fileStorageService.getStorageStats());
    }
}
