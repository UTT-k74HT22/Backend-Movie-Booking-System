package com.trainning.movie_booking_system.config;

import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.entity.AccountHasRole;
import com.trainning.movie_booking_system.entity.Role;
import com.trainning.movie_booking_system.entity.User;
import com.trainning.movie_booking_system.repository.AccountRepository;
import com.trainning.movie_booking_system.repository.RoleRepository;
import com.trainning.movie_booking_system.repository.UserRepository;
import com.trainning.movie_booking_system.untils.enums.RoleType;
import com.trainning.movie_booking_system.untils.enums.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Slf4j
public class ApplicationInitConfig {

    @Bean
    public ApplicationRunner applicationRunner(
            AccountRepository accountRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        
        log.info("Initializing application with default data...");
        
        return args -> {
            // Tạo roles nếu chưa tồn tại
            createRolesIfNotExists(roleRepository);
            
            // Tạo admin account nếu chưa tồn tại
            createAdminIfNotExists(accountRepository, userRepository, roleRepository, passwordEncoder);
        };
    }

    private void createRolesIfNotExists(RoleRepository roleRepository) {
        log.info("Creating roles if not exists...");
        
        if (roleRepository.findByName(RoleType.USER).isEmpty()) {
            Role userRole = Role.builder()
                    .name(RoleType.USER)
                    .description("Người dùng thông thường")
                    .build();
            roleRepository.save(userRole);
            log.info("Created role: USER");
        }
        
        if (roleRepository.findByName(RoleType.STAFF).isEmpty()) {
            Role staffRole = Role.builder()
                    .name(RoleType.STAFF)
                    .description("Nhân viên")
                    .build();
            roleRepository.save(staffRole);
            log.info("Created role: STAFF");
        }
        
        if (roleRepository.findByName(RoleType.THEATER_MANAGEMENT).isEmpty()) {
            Role theaterRole = Role.builder()
                    .name(RoleType.THEATER_MANAGEMENT)
                    .description("Quản lý rạp chiếu phim")
                    .build();
            roleRepository.save(theaterRole);
            log.info("Created role: THEATER_MANAGEMENT");
        }
        
        if (roleRepository.findByName(RoleType.ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                    .name(RoleType.ADMIN)
                    .description("Quản trị viên hệ thống")
                    .build();
            roleRepository.save(adminRole);
            log.info("Created role: ADMIN");
        }
    }

    private void createAdminIfNotExists(
            AccountRepository accountRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        
        log.info("Creating admin account if not exists...");
        
        if (accountRepository.findByUsername("admin").isEmpty()) {
            // Tạo Account admin
            Account adminAccount = Account.builder()
                    .username("admin")
                    .email("admin@moviebooking.com")
                    .password(passwordEncoder.encode("admin123"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .build();

            // Lấy role ADMIN
            Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

            // Tạo AccountHasRole
            AccountHasRole accountRole = AccountHasRole.builder()
                    .account(adminAccount)
                    .role(adminRole)
                    .build();

            // Gắn role vào account
            adminAccount.getAccountRoles().add(accountRole);

            // Lưu Account
            Account savedAccount = accountRepository.save(adminAccount);
            log.info("Admin account created successfully with ID: {}", savedAccount.getId());

            // Tạo User profile cho admin
            User adminUser = User.builder()
                    .account(savedAccount)
                    .firstName("Admin")
                    .lastName("System")
                    .phoneNumber("0123456789")
                    .build();

            userRepository.save(adminUser);
            log.warn("Admin user has been created with default password: admin123. Please change it immediately.");
            log.info("Admin credentials - Username: admin, Password: admin123");
        } else {
            log.info("Admin account already exists");
        }
    }
}
