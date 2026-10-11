package com.b2b.warehouse.branch.repository;

import com.b2b.warehouse.branch.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    Optional<BranchEntity> findByCodeIgnoreCase(String code);

    List<BranchEntity> findAllByIsActiveTrueOrderByIsPinnedDescNameAsc();

    List<BranchEntity> findAllByOrderByIsPinnedDescNameAsc();
}
