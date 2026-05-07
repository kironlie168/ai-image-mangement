package com.aiimage.repository;

import com.aiimage.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByParentIdIsNull();
    List<Folder> findByParentId(Long parentId);
}
