package com.b2b.warehouse.partner.repository;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.partner.entity.B2bPartnerProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface B2bPartnerProfileRepository extends JpaRepository<B2bPartnerProfileEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "user.role"})
    Optional<B2bPartnerProfileEntity> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "user.role"})
    Optional<B2bPartnerProfileEntity> findWithUserById(UUID id);

    @EntityGraph(attributePaths = {"user", "user.role"})
    List<B2bPartnerProfileEntity> findByIdIn(Collection<UUID> ids);

    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndUserIdNot(String taxCode, UUID userId);

    /**
     * Admin Partner CRM search and status filter with single-query JOIN FETCH on user & role.
     */
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT p FROM B2bPartnerProfileEntity p
            JOIN p.user u
            WHERE u.deletedAt IS NULL
              AND (:status IS NULL OR u.accountStatus = :status)
              AND (
                   :keyword IS NULL OR :keyword = ''
                   OR LOWER(p.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.taxCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY p.createdAt DESC
            """)
    Page<B2bPartnerProfileEntity> searchPartnersForAdmin(
            @Param("status") AccountStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
