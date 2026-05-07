package com.aiimage.repository;

import com.aiimage.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByNameContainingIgnoreCase(String name);

    long countByIdIn(List<Long> ids);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM image_tag WHERE tag_id = ?1", nativeQuery = true)
    void deleteImageAssociations(Long tagId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM tag WHERE id = ?1", nativeQuery = true)
    void deleteByIdNative(Long tagId);
}
