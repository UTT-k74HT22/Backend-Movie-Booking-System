package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.entity.RolePermission;
import com.trainning.movie_booking_system.service.RolePermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionController {

    RolePermissionService rolePermissionService;

    @PostMapping("/assign")
    public ResponseEntity<RolePermission> assignPermissionToRole(
            @RequestParam Long roleId,
            @RequestParam Long permissionId
    ) {
        RolePermission rp = rolePermissionService.assignPermissionToRole(roleId, permissionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(rp);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removePermissionFromRole(
            @RequestParam Long roleId,
            @RequestParam Long permissionId
    ) {
        rolePermissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<RolePermission>> getPermissionsByRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(rolePermissionService.getPermissionsByRole(roleId));
    }

    @GetMapping("/permission/{permissionId}")
    public ResponseEntity<List<RolePermission>> getRolesByPermission(@PathVariable Long permissionId) {
        return ResponseEntity.ok(rolePermissionService.getRolesByPermission(permissionId));
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> roleHasPermission(
            @RequestParam Long roleId,
            @RequestParam Long permissionId
    ) {
        return ResponseEntity.ok(rolePermissionService.roleHasPermission(roleId, permissionId));
    }
}



