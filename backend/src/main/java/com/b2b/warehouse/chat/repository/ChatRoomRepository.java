package com.b2b.warehouse.chat.repository;

import com.b2b.warehouse.chat.entity.ChatRoomEntity;
import com.b2b.warehouse.chat.entity.ChatRoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, UUID> {

    @EntityGraph(attributePaths = {"partnerUser", "assignedAdmin"})
    Optional<ChatRoomEntity> findByPartnerUser_Id(UUID partnerUserId);

    @EntityGraph(attributePaths = {"partnerUser", "assignedAdmin"})
    Optional<ChatRoomEntity> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"partnerUser", "assignedAdmin"})
    @Query("""
            SELECT r FROM ChatRoomEntity r
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.lastMessageAt DESC
            """)
    Page<ChatRoomEntity> findRoomsForAdmin(@Param("status") ChatRoomStatus status, Pageable pageable);

    @Query("""
            SELECT COUNT(m) FROM ChatMessageEntity m
            WHERE m.room.id = :roomId
              AND m.sender.id != :userId
              AND m.isRead = FALSE
            """)
    long countUnreadMessagesForUser(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}
