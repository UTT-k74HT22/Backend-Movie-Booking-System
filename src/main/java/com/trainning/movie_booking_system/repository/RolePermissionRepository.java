package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.RolePermission;
import com.trainning.movie_booking_system.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    
    List<RolePermission> findByRoleId(Long roleId);
    
    List<RolePermission> findByPermissionId(Long permissionId);
    
    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
    
    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
