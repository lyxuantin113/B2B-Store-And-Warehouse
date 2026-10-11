package com.b2b.warehouse.rbac.repository;

import com.b2b.warehouse.rbac.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Short> {

    List<PermissionEntity> findByCodeIn(Collection<String> codes);
}
