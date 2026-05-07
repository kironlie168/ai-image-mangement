package com.aiimage.service;

import com.aiimage.dto.TagDto;
import com.aiimage.entity.Tag;
import com.aiimage.exception.ResourceNotFoundException;
import com.aiimage.repository.TagRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    private static final Map<String, String> DEFAULT_TAGS = Map.ofEntries(
            Map.entry("人像", "#f43f5e"),
            Map.entry("风景", "#10b981"),
            Map.entry("科幻", "#6366f1"),
            Map.entry("奇幻", "#a855f7"),
            Map.entry("二次元", "#ec4899"),
            Map.entry("写实", "#f59e0b"),
            Map.entry("概念艺术", "#8b5cf6"),
            Map.entry("建筑", "#0ea5e9"),
            Map.entry("动物", "#84cc16"),
            Map.entry("抽象", "#f97316")
    );

    @PostConstruct
    void seedDefaultTags() {
        if (tagRepository.count() > 0) return;
        log.info("Seeding default tags...");
        DEFAULT_TAGS.forEach((name, color) -> {
            Tag tag = Tag.builder().name(name).color(color).build();
            tagRepository.save(tag);
        });
        log.info("Seeded {} default tags", DEFAULT_TAGS.size());
    }

    public List<TagDto> findAll() {
        return tagRepository.findAll().stream().map(this::toDto).toList();
    }

    public TagDto create(String name, String color) {
        Tag tag = Tag.builder()
                .name(name)
                .color(color != null ? color : "#1890ff")
                .build();
        return toDto(tagRepository.save(tag));
    }

    public TagDto update(Long id, String name, String color) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.tag(id));
        if (name != null) tag.setName(name);
        if (color != null) tag.setColor(color);
        return toDto(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw ResourceNotFoundException.tag(id);
        }
        tagRepository.deleteImageAssociations(id);
        tagRepository.deleteByIdNative(id);
    }

    public List<TagDto> search(String keyword) {
        return tagRepository.findByNameContainingIgnoreCase(keyword)
                .stream().map(this::toDto).toList();
    }

    private TagDto toDto(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getColor());
    }
}
