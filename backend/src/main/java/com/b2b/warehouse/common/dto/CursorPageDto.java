package com.b2b.warehouse.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Keyset (Seek/Cursor) pagination response DTO.
 * Replaces slow OFFSET pagination with O(log N) index seek using (created_at, id).
 *
 * @param <T> Item DTO type
 */
public record CursorPageDto<T>(
        List<T> items,
        Instant nextCursorCreatedAt,
        UUID nextCursorId,
        boolean hasNext
) {
    public static <T> CursorPageDto<T> empty() {
        return new CursorPageDto<>(List.of(), null, null, false);
    }
}
