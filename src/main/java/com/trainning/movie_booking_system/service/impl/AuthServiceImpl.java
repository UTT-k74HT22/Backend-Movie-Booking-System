package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.security.JwtProvider;
import com.trainning.movie_booking_system.service.AuthService;
import com.trainning.movie_booking_system.untils.enums.RoleType;
import com.trainning.movie_booking_system.untils.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.AuthenticationException;

import static com.trainning.movie_booking_system.mapper.AuthMapper.toResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("Starting registration for username: {}", request.getUsername());

        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username đã tồn tại: " + request.getUsername());
        }

        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại: " + request.getEmail());
        }

        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        // Lấy role mặc định USER
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        // Tạo bản ghi trung gian
        AccountHasRole accountRole = AccountHasRole.builder()
                .account(account)
                .role(userRole)
                .build();

        // Gắn role vào account (accountRoles đã được khởi tạo trong entity)
        account.getAccountRoles().add(accountRole);

        // Lưu Account (cascade lưu cả AccountHasRole)
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());

        // Tạo profile User
        User user = User.builder()
                .account(savedAccount)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        userRepository.save(user);
        log.info("User profile created successfully for account: {}", savedAccount.getUsername());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Starting login for username: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomAccountDetails accountDetails = (CustomAccountDetails) authentication.getPrincipal();
        Account account = accountDetails.account();

        if (!account.isEmailVerified() || account.getStatus().equals(UserStatus.LOCKED)) {
            throw new RuntimeException("Account is locked");
        }

        log.info("Authentication successful for username: {}", account.getUsername());

        String accessToken = jwtProvider.generateToken(account);
        String refreshToken = jwtProvider.generateRefreshToken(account);

        return toResponse(accessToken, refreshToken);
    }
}
        