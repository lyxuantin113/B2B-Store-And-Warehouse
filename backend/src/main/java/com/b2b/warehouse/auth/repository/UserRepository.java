package com.b2b.warehouse.auth.repository;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Fetches user, role, and role permissions in a single SQL LEFT JOIN query.
     * Eliminates N+1 queries during login and token refresh.
     */
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<UserEntity> findWithRoleByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole_CodeAndAccountStatusAndDeletedAtIsNull(String roleCode, AccountStatus accountStatus);

    long countByRole_CodeAndDeletedAtIsNull(String roleCode);
}
