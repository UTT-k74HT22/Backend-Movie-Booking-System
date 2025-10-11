package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.entity.Permission;
import com.trainning.movie_booking_system.entity.Role;
import com.trainning.movie_booking_system.entity.RolePermission;
import com.trainning.movie_booking_system.repository.PermissionRepository;
import com.trainning.movie_booking_system.repository.RolePermissionRepository;
import com.trainning.movie_booking_system.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionService {

    RolePermissionRepository rolePermissionRepository;
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;

    public RolePermission assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            return RolePermission.builder().role(role).permission(permission).build();
        }

        RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .build();
        return rolePermissionRepository.save(rolePermission);
    }

    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    public List<RolePermission> getPermissionsByRole(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }

    public List<RolePermission> getRolesByPermission(Long permissionId) {
        return rolePermissionRepository.findByPermissionId(permissionId);
    }

    public boolean roleHasPermission(Long roleId, Long permissionId) {
        return rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }
}


