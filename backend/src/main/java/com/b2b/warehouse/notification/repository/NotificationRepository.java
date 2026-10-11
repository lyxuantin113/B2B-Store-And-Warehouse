package com.b2b.warehouse.notification.repository;

import com.b2b.warehouse.notification.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * Uses partial composite index {@code idx_notifications_user_unread_created}
     * ({@code WHERE is_read = FALSE}) for instant O(1) badge counter rendering.
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    Optional<NotificationEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT n FROM NotificationEntity n
            WHERE n.userId = :userId
              AND (:unreadOnly = FALSE OR n.isRead = FALSE)
              AND (
                   :cursorCreatedAt IS NULL
                   OR n.createdAt < :cursorCreatedAt
                   OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)
              )
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<NotificationEntity> findUserNotificationsKeyset(
            @Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationEntity n
            SET n.isRead = TRUE,
                n.readAt = :readAt
            WHERE n.userId = :userId
              AND n.isRead = FALSE
            """)
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
