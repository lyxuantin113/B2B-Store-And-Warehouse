package com.b2b.warehouse.branch.repository;

import com.b2b.warehouse.branch.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    Optional<BranchEntity> findByCodeIgnoreCase(String code);

    List<BranchEntity> findAllByIsActiveTrueOrderByIsPinnedDescNameAsc();

    List<BranchEntity> findAllByOrderByIsPinnedDescNameAsc();

    /**
     * PostGIS Spatial Radius Search (uses partial GIST index {@code idx_branches_geom_gist}).
     * Finds active branches within {@code radiusMeters} of the target {@code (longitude, latitude)} point,
     * sorted by pinned priority and exact spherical distance.
     */
    @Query(value = """
            SELECT b.*
            FROM branches b
            WHERE b.is_active = TRUE
              AND ST_DWithin(
                  b.geom,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                  :radiusMeters
              )
            ORDER BY b.is_pinned DESC,
                     ST_Distance(
                         b.geom,
                         ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                     ) ASC
            """, nativeQuery = true)
    List<BranchEntity> findNearbyBranchesWithinRadiusMeters(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );
}
