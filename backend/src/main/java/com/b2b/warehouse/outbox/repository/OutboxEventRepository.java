package com.b2b.warehouse.outbox.repository;

import com.b2b.warehouse.outbox.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Uses the partial index {@code idx_outbox_pending_created} to poll unpublished events in FIFO order.
     */
    @Query("""
            SELECT e FROM OutboxEventEntity e
            WHERE e.status = 'PENDING'
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEventEntity> findPendingEventsBatch(Pageable pageable);
}
