package com.aiimage.service;

import com.aiimage.dto.FolderDto;
import com.aiimage.entity.Folder;
import com.aiimage.exception.ResourceNotFoundException;
import com.aiimage.repository.AiImageRepository;
import com.aiimage.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final AiImageRepository imageRepository;

    public List<FolderDto> getTree() {
        List<Folder> allFolders = folderRepository.findAll();
        Map<Long, List<Folder>> childrenByParentId = allFolders.stream()
                .filter(f -> f.getParent() != null)
                .collect(Collectors.groupingBy(f -> f.getParent().getId()));
        Map<Long, Long> imageCounts = allFolders.stream()
                .collect(Collectors.toMap(Folder::getId, f -> imageRepository.countByFolderId(f.getId())));

        return allFolders.stream()
                .filter(f -> f.getParent() == null)
                .map(f -> buildTreeDto(f, childrenByParentId, imageCounts))
                .toList();
    }

    public List<FolderDto> getFlatList() {
        return folderRepository.findAll().stream()
                .map(f -> new FolderDto(f.getId(), f.getName(),
                        f.getParent() != null ? f.getParent().getId() : null,
                        f.getDescription(),
                        (int) imageRepository.countByFolderId(f.getId()),
                        List.of(),
                        f.getCreatedAt()))
                .toList();
    }

    public FolderDto create(String name, Long parentId, String description) {
        Folder folder = Folder.builder()
                .name(name)
                .description(description)
                .build();
        if (parentId != null) {
            Folder parent = folderRepository.findById(parentId)
                    .orElseThrow(() -> ResourceNotFoundException.folder(parentId));
            folder.setParent(parent);
        }
        return toDto(folderRepository.save(folder));
    }

    public FolderDto update(Long id, String name, Long parentId, String description) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.folder(id));
        if (name != null) folder.setName(name);
        if (description != null) folder.setDescription(description);
        if (parentId != null) {
            Folder parent = folderRepository.findById(parentId)
                    .orElseThrow(() -> ResourceNotFoundException.folder(parentId));
            folder.setParent(parent);
        } else if (parentId == null && folder.getParent() != null) {
            folder.setParent(null);
        }
        return toDto(folderRepository.save(folder));
    }

    public void delete(Long id) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.folder(id));
        folderRepository.delete(folder);
    }

    private FolderDto buildTreeDto(Folder folder, Map<Long, List<Folder>> childrenMap,
                                    Map<Long, Long> imageCounts) {
        List<FolderDto> childDtos = childrenMap.getOrDefault(folder.getId(), List.of())
                .stream()
                .map(child -> buildTreeDto(child, childrenMap, imageCounts))
                .toList();
        return new FolderDto(
                folder.getId(), folder.getName(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getDescription(),
                imageCounts.getOrDefault(folder.getId(), 0L).intValue(),
                childDtos,
                folder.getCreatedAt()
        );
    }

    private FolderDto toDto(Folder folder) {
        return new FolderDto(
                folder.getId(), folder.getName(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getDescription(),
                (int) imageRepository.countByFolderId(folder.getId()),
                List.of(),
                folder.getCreatedAt()
        );
    }
}
