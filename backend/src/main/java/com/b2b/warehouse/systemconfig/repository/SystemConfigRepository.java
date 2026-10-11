package com.b2b.warehouse.systemconfig.repository;

import com.b2b.warehouse.systemconfig.entity.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, String> {

    List<SystemConfigEntity> findAllByOrderByConfigGroupAscConfigKeyAsc();

    List<SystemConfigEntity> findAllByConfigGroupIgnoreCaseOrderByConfigKeyAsc(String configGroup);
}
