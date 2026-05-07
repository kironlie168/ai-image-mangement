package com.aiimage.repository;

import com.aiimage.entity.AiImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiImageRepository extends JpaRepository<AiImage, Long> {

    @Query("SELECT i FROM AiImage i WHERE " +
           "(:keyword IS NULL OR i.prompt LIKE %:keyword% OR i.negativePrompt LIKE %:keyword% OR i.model LIKE %:keyword% OR i.originalFilename LIKE %:keyword%) " +
           "AND (:folderId IS NULL OR i.folder.id = :folderId) " +
           "AND (:favorite IS NULL OR i.isFavorite = :favorite) " +
           "AND (:tagIds IS NULL OR i.id IN (SELECT it.id FROM AiImage it JOIN it.tags t WHERE t.id IN :tagIds))")
    Page<AiImage> searchImages(@Param("keyword") String keyword,
                               @Param("folderId") Long folderId,
                               @Param("favorite") Boolean favorite,
                               @Param("tagIds") List<Long> tagIds,
                               Pageable pageable);

    Page<AiImage> findByFolderId(Long folderId, Pageable pageable);

    Page<AiImage> findByIsFavoriteTrue(Pageable pageable);

    Optional<AiImage> findByStoredFilename(String storedFilename);

    long countByFolderId(Long folderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM image_tag WHERE image_id = :imageId AND tag_id IN :tagIds", nativeQuery = true)
    int removeTagsFromImage(@Param("imageId") Long imageId, @Param("tagIds") List<Long> tagIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM image_tag WHERE image_id IN :imageIds AND tag_id IN :tagIds", nativeQuery = true)
    int batchRemoveTags(@Param("imageIds") List<Long> imageIds, @Param("tagIds") List<Long> tagIds);

    long countByIdIn(List<Long> ids);
}
