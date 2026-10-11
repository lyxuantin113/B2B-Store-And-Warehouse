package com.b2b.warehouse.rbac.service;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.auth.repository.UserRepository;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.rbac.dto.CreateStaffAccountRequestDto;
import com.b2b.warehouse.rbac.dto.RolePermissionsResponseDto;
import com.b2b.warehouse.rbac.dto.UpdateRolePermissionsRequestDto;
import com.b2b.warehouse.rbac.entity.PermissionEntity;
import com.b2b.warehouse.rbac.entity.RoleCode;
import com.b2b.warehouse.rbac.entity.RoleEntity;
import com.b2b.warehouse.rbac.repository.PermissionRepository;
import com.b2b.warehouse.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<RolePermissionsResponseDto> getAllRolesWithPermissions() {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .map(this::toRoleDto)
                .toList();
    }

    @Transactional
    public RolePermissionsResponseDto updateRolePermissions(String roleCode, UpdateRolePermissionsRequestDto request) {
        if (RoleCode.ADMIN.name().equalsIgnoreCase(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "System ADMIN role permissions cannot be stripped");
        }

        RoleEntity role = roleRepository.findByCode(roleCode.toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found: " + roleCode));

        List<PermissionEntity> permissions = permissionRepository.findByCodeIn(request.permissionCodes());
        role.setPermissions(new HashSet<>(permissions));
        roleRepository.save(role);

        return toRoleDto(role);
    }

    @Transactional
    public Map<String, Object> createStaffAccount(CreateStaffAccountRequestDto request) {
        if (request.roleCode() == RoleCode.B2B_PARTNER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Use /api/v1/auth/register-b2b to register B2B Partner accounts");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        RoleEntity role = roleRepository.findByCode(request.roleCode().name())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found: " + request.roleCode()));

        UserEntity staffUser = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .tokenVersion(1)
                .build();
        staffUser = userRepository.save(staffUser);

        UUID userId = staffUser.getId();
        return Map.of(
                "userId", userId != null ? userId.toString() : "",
                "email", staffUser.getEmail(),
                "fullName", staffUser.getFullName(),
                "role", role.getCode(),
                "accountStatus", staffUser.getAccountStatus().name()
        );
    }

    private RolePermissionsResponseDto toRoleDto(RoleEntity role) {
        List<String> permCodes = role.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .sorted()
                .toList();
        return RolePermissionsResponseDto.builder()
                .roleId(role.getId())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .description(role.getDescription())
                .permissions(permCodes)
                .build();
    }
}
