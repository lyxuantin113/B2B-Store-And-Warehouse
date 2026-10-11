package com.b2b.warehouse.branch.service;

import com.b2b.warehouse.branch.dto.BranchResponseDto;
import com.b2b.warehouse.branch.dto.NearbyBranchResponseDto;
import com.b2b.warehouse.branch.dto.StandardizedAddressDto;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final int WGS84_SRID = 4326;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private final BranchRepository branchRepository;
    private final GoogleMapsGeocodingService googleMapsGeocodingService;

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

    /**
     * Searches nearest active store/warehouse branches by GPS coordinates or Vietnamese address string
     * within a given radius in kilometers (Store Locator - {@code stores.php}).
     */
    @Transactional(readOnly = true)
    public List<NearbyBranchResponseDto> searchNearestBranches(
            Double latitude,
            Double longitude,
            String addressQuery,
            Double radiusKm,
            String categoryTag,
            String keyword
    ) {
        double targetLat;
        double targetLng;

        if (latitude != null && longitude != null) {
            targetLat = latitude;
            targetLng = longitude;
        } else if (addressQuery != null && !addressQuery.isBlank()) {
            StandardizedAddressDto geocoded = googleMapsGeocodingService.standardizeAddress(addressQuery);
            targetLat = geocoded.latitude();
            targetLng = geocoded.longitude();
        } else {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Either (latitude, longitude) or addressQuery must be provided for nearest store search"
            );
        }

        double effectiveRadiusKm = (radiusKm == null || radiusKm <= 0) ? 10.0 : Math.min(radiusKm, 500.0);
        double radiusMeters = effectiveRadiusKm * 1000.0;

        List<BranchEntity> candidates = branchRepository.findNearbyBranchesWithinRadiusMeters(
                targetLat,
                targetLng,
                radiusMeters
        );

        String normalizedCategory = (categoryTag == null || categoryTag.isBlank())
                ? null
                : categoryTag.trim().toLowerCase(Locale.ROOT);
        String normalizedKeyword = (keyword == null || keyword.isBlank())
                ? null
                : keyword.trim().toLowerCase(Locale.ROOT);

        return candidates.stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> normalizedCategory == null
                        || (b.getCategoryTag() != null && b.getCategoryTag().toLowerCase(Locale.ROOT).equals(normalizedCategory)))
                .filter(b -> normalizedKeyword == null
                        || b.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || b.getAddressLine().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .map((BranchEntity b) -> {
                    long distMeters = Math.round(calculateHaversineMeters(
                            targetLat, targetLng, b.getLatitude(), b.getLongitude()
                    ));
                    double distKm = BigDecimal.valueOf(distMeters / 1000.0)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
                    return NearbyBranchResponseDto.builder()
                            .id(b.getId())
                            .code(b.getCode())
                            .name(b.getName())
                            .categoryTag(b.getCategoryTag())
                            .phone(b.getPhone())
                            .postalCode(b.getPostalCode())
                            .addressLine(b.getAddressLine())
                            .addressDetail(b.getAddressDetail())
                            .latitude(b.getLatitude())
                            .longitude(b.getLongitude())
                            .isPinned(Boolean.TRUE.equals(b.getIsPinned()))
                            .isActive(Boolean.TRUE.equals(b.getIsActive()))
                            .operatingHours(b.getOperatingHours())
                            .distanceKm(distKm)
                            .distanceMeters(distMeters)
                            .build();
                })
                .filter((NearbyBranchResponseDto dto) -> dto.distanceMeters() <= Math.round(radiusMeters))
                .sorted(Comparator.<NearbyBranchResponseDto, Boolean>comparing(NearbyBranchResponseDto::isPinned).reversed()
                        .thenComparingLong(NearbyBranchResponseDto::distanceMeters))
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

    public static double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
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
