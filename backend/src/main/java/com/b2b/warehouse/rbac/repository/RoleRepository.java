package com.b2b.warehouse.rbac.repository;

import com.b2b.warehouse.rbac.entity.RoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Short> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<RoleEntity> findByCode(String code);

    @EntityGraph(attributePaths = {"permissions"})
    List<RoleEntity> findAllByOrderByIdAsc();
}
