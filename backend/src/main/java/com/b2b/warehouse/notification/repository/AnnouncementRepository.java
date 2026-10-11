package com.b2b.warehouse.notification.repository;

import com.b2b.warehouse.notification.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, UUID> {

    /**
     * Uses partial composite index {@code idx_announcements_active_pinned_pub}
     * ({@code is_published = TRUE AND deleted_at IS NULL ORDER BY is_pinned DESC, published_at DESC}).
     */
    List<AnnouncementEntity> findAllByIsPublishedTrueAndDeletedAtIsNullOrderByIsPinnedDescPublishedAtDesc();

    List<AnnouncementEntity> findAllByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc();

    Optional<AnnouncementEntity> findByIdAndDeletedAtIsNull(UUID id);
}
