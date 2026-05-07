package com.aiimage.controller;

import com.aiimage.dto.CreateTagRequest;
import com.aiimage.dto.TagDto;
import com.aiimage.dto.UpdateTagRequest;
import com.aiimage.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Tag CRUD and search")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Get all tags")
    public ResponseEntity<List<TagDto>> getAllTags() {
        return ResponseEntity.ok(tagService.findAll());
    }

    @GetMapping("/search")
    @Operation(summary = "Search tags by keyword")
    public ResponseEntity<List<TagDto>> searchTags(@RequestParam @Parameter(description = "Search keyword") String q) {
        return ResponseEntity.ok(tagService.search(q));
    }

    @PostMapping
    @Operation(summary = "Create tag")
    public ResponseEntity<TagDto> createTag(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.ok(tagService.create(request.name(), request.color()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tag")
    public ResponseEntity<TagDto> updateTag(@PathVariable @Parameter(description = "Tag ID") Long id, @Valid @RequestBody UpdateTagRequest request) {
        return ResponseEntity.ok(tagService.update(id, request.name(), request.color()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tag")
    public ResponseEntity<Void> deleteTag(@PathVariable @Parameter(description = "Tag ID") Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
