package com.aiimage.controller;

import com.aiimage.dto.FolderDto;
import com.aiimage.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Tag(name = "Folders", description = "Folder tree management")
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/tree")
    @Operation(summary = "Get folder tree", description = "Get all folders as a nested tree structure with image counts")
    public ResponseEntity<List<FolderDto>> getTree() {
        return ResponseEntity.ok(folderService.getTree());
    }

    @GetMapping
    @Operation(summary = "Get flat folder list")
    public ResponseEntity<List<FolderDto>> getFlatList() {
        return ResponseEntity.ok(folderService.getFlatList());
    }

    @PostMapping
    @Operation(summary = "Create folder")
    public ResponseEntity<FolderDto> createFolder(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;
        String description = (String) body.get("description");
        return ResponseEntity.ok(folderService.create(name, parentId, description));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update folder")
    public ResponseEntity<FolderDto> updateFolder(@PathVariable @Parameter(description = "Folder ID") Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;
        String description = (String) body.get("description");
        return ResponseEntity.ok(folderService.update(id, name, parentId, description));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete folder")
    public ResponseEntity<Void> deleteFolder(@PathVariable @Parameter(description = "Folder ID") Long id) {
        folderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
