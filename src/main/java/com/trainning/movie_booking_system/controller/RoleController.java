package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.ApiResponse;
import com.trainning.movie_booking_system.entity.Role;
import com.trainning.movie_booking_system.exception.AppException;
import com.trainning.movie_booking_system.exception.ErrorCode;
import com.trainning.movie_booking_system.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

    RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        ApiResponse<List<Role>> response = ApiResponse.<List<Role>>builder()
                .success(true)
                .message("Roles retrieved successfully")
                .result(roles)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        
        ApiResponse<Role> response = ApiResponse.<Role>builder()
                .success(true)
                .message("Role retrieved successfully")
                .result(role)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<Role>> getRoleByName(@PathVariable String name) {
        Role role = roleService.getRoleByName(name)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        
        ApiResponse<Role> response = ApiResponse.<Role>builder()
                .success(true)
                .message("Role retrieved successfully")
                .result(role)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Role role) {
        Role created = roleService.createRole(role);
        ApiResponse<Role> response = ApiResponse.<Role>builder()
                .success(true)
                .message("Role created successfully")
                .result(created)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        Role updated = roleService.updateRole(role);
        ApiResponse<Role> response = ApiResponse.<Role>builder()
                .success(true)
                .message("Role updated successfully")
                .result(updated)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Role deleted successfully")
                .result(null)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> existsByName(@RequestParam String name) {
        boolean exists = roleService.existsByName(name);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .message("Role existence checked successfully")
                .result(exists)
                .build();
        return ResponseEntity.ok(response);
    }
}


