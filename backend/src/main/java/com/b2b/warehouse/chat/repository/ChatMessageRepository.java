package com.b2b.warehouse.chat.repository;

import com.b2b.warehouse.chat.entity.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * Keyset pagination for room messages using index {@code idx_chat_messages_room_id_desc (room_id, id DESC)}.
     */
    @EntityGraph(attributePaths = {"sender", "sender.role"})
    @Query("""
            SELECT m FROM ChatMessageEntity m
            WHERE m.room.id = :roomId
              AND (:cursorMessageId IS NULL OR m.id < :cursorMessageId)
            ORDER BY m.id DESC
            """)
    List<ChatMessageEntity> findMessagesKeyset(
            @Param("roomId") UUID roomId,
            @Param("cursorMessageId") Long cursorMessageId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender", "sender.role"})
    Optional<ChatMessageEntity> findTop1ByRoom_IdOrderByIdDesc(UUID roomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessageEntity m
            SET m.isRead = TRUE
            WHERE m.room.id = :roomId
              AND m.sender.id != :readerId
              AND m.isRead = FALSE
            """)
    int markRoomMessagesAsRead(@Param("roomId") UUID roomId, @Param("readerId") UUID readerId);
}
