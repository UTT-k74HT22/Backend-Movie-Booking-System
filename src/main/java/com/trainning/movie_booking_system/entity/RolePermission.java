package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}),
        indexes = {
                @Index(name = "idx_role_permission_role_id", columnList = "role_id"),
                @Index(name = "idx_role_permission_permission_id", columnList = "permission_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}
