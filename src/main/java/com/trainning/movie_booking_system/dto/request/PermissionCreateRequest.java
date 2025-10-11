package com.trainning.movie_booking_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCreateRequest {
    
    @NotBlank(message = "Permission name is required")
    @Size(min = 2, max = 50, message = "Permission name must be between 2 and 50 characters")
    private String name;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @Size(max = 50, message = "Module name must not exceed 50 characters")
    private String module;
}
