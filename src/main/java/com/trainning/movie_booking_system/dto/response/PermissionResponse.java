package com.trainning.movie_booking_system.dto.response;

import com.trainning.movie_booking_system.entity.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    
    private Long id;
    private String name;
    private String description;
    private String module;
    private LocalDateTime createdAt;
    
    public static PermissionResponse fromEntity(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(permission.getModule())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
