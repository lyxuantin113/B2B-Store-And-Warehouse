package com.b2b.warehouse.branch.service;

import com.b2b.warehouse.branch.dto.BranchResponseDto;
import com.b2b.warehouse.branch.dto.UpsertBranchRequestDto;
import com.b2b.warehouse.branch.entity.BranchEntity;
import com.b2b.warehouse.branch.repository.BranchRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchResponseDto> listAllBranchesForAdmin() {
        return branchRepository.findAllByOrderByIsPinnedDescNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchResponseDto> listActiveBranches() {
        return branchRepository.findAllByIsActiveTrueOrderByIsPinnedDescNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public BranchResponseDto createBranch(UpsertBranchRequestDto request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (branchRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Branch code already exists: " + normalizedCode);
        }

        BranchEntity entity = BranchEntity.builder()
                .code(normalizedCode)
                .name(request.name().trim())
                .categoryTag(request.categoryTag())
                .phone(request.phone())
                .postalCode(request.postalCode())
                .addressLine(request.addressLine().trim())
                .addressDetail(request.addressDetail())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .geom(createPoint(request.longitude(), request.latitude()))
                .isPinned(Boolean.TRUE.equals(request.isPinned()))
                .isActive(request.isActive() == null || request.isActive())
                .operatingHours(request.operatingHours())
                .build();

        return toDto(branchRepository.save(entity));
    }

    @Transactional
    public BranchResponseDto updateBranch(UUID branchId, UpsertBranchRequestDto request) {
        BranchEntity entity = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));

        String normalizedCode = request.code().trim().toUpperCase();
        if (branchRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, branchId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Branch code already exists: " + normalizedCode);
        }

        entity.setCode(normalizedCode);
        entity.setName(request.name().trim());
        entity.setCategoryTag(request.categoryTag());
        entity.setPhone(request.phone());
        entity.setPostalCode(request.postalCode());
        entity.setAddressLine(request.addressLine().trim());
        entity.setAddressDetail(request.addressDetail());
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setGeom(createPoint(request.longitude(), request.latitude()));
        if (request.isPinned() != null) {
            entity.setIsPinned(request.isPinned());
        }
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }
        entity.setOperatingHours(request.operatingHours());

        return toDto(branchRepository.save(entity));
    }

    public static Point createPoint(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(WGS84_SRID);
        return point;
    }

    private BranchResponseDto toDto(BranchEntity entity) {
        return BranchResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .categoryTag(entity.getCategoryTag())
                .phone(entity.getPhone())
                .postalCode(entity.getPostalCode())
                .addressLine(entity.getAddressLine())
                .addressDetail(entity.getAddressDetail())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .isPinned(Boolean.TRUE.equals(entity.getIsPinned()))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .operatingHours(entity.getOperatingHours())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
