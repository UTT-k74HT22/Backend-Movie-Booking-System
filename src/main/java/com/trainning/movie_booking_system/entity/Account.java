package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.untils.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_username", columnList = "username"),
                @Index(name = "idx_account_email", columnList = "email")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @OneToMany(mappedBy = "account",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private Set<AccountHasRole> accountRoles;
}
