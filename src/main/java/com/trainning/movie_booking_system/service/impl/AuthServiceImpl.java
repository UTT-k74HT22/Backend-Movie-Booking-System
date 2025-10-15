package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.security.JwtProvider;
import com.trainning.movie_booking_system.service.*;
import com.trainning.movie_booking_system.untils.enums.OtpType;
import com.trainning.movie_booking_system.untils.enums.RoleType;
import com.trainning.movie_booking_system.untils.enums.UserStatus;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final RedisService redisService;
    private final OtpService otpService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("Starting registration for username: {}", request.getUsername());

        validateField(request);

        Account account = buildAccount(request);

        // Lấy role mặc định USER
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        // Tạo bản ghi trung gian
        AccountHasRole accountRole = buildAccountRole(account, userRole);

        // Gắn role vào account (accountRoles đã được khởi tạo trong entity)
        account.getAccountRoles().add(accountRole);

        // Lưu Account (cascade lưu cả AccountHasRole)
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());

        // Tạo profile User
        User user = buildProfileUser(request, savedAccount);

        userRepository.save(user);
        log.info("User profile created successfully for account: {}", savedAccount.getUsername());

        otpService.sendOtp(request.getEmail(), OtpType.REGISTER);

        log.info("Registration successful for {}, awaiting OTP verification", request.getEmail());
    }

    /**
     * Active email
     *
     * @param request thông tin request
     */
    @Override
    @Transactional
    public void activateAccount(VerifyOtpRequest request) {
        log.info("Activating account for email: {}", request.getEmail());

        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.REGISTER);
        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        account.setEmailVerified(true);
        accountRepository.save(account);

        otpService.deleteOtp(request.getEmail(), OtpType.REGISTER);

        log.info("Account {} activated successfully", account.getUsername());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Starting login for username: {}", request.getUsername());

        Account account = authenticationAndValidateAccount(request);
        String accessToken = jwtProvider.generateToken(account);
        String refreshToken = jwtProvider.generateRefreshToken(account);
        String key = "refreshToken:" + account.getUsername();
        long ttl = (jwtProvider.getExpiration(refreshToken).getTime() - System.currentTimeMillis()) / 1000;
        redisService.set(key, refreshToken, ttl, TimeUnit.SECONDS);

        return toResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Starting refresh token process");

        log.info("Refresh Token: {}", refreshToken);

        String cleanToken = refreshToken.trim().replace("\"", "");

        log.info("Refresh Token Clean: {}", cleanToken);

        if (!jwtProvider.validateToken(cleanToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        try {
            String username = jwtProvider.extractUsername(cleanToken);
            log.debug("Extracted username from refresh token: {}", username);

            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("User not found"));

            String redisKey = "refreshToken:" + username;
            String storedRefreshToken = redisService.get(redisKey).toString();

            if (storedRefreshToken == null) {
                throw new BadRequestException("Invalid or expired refresh token");
            }

            storedRefreshToken = storedRefreshToken.trim().replace("\"", "");

            if (!storedRefreshToken.equals(cleanToken)) {
                throw new BadRequestException("Invalid or expired refresh token");
            }

            if (!jwtProvider.isTokenValidForAccount(cleanToken, account)) {
                throw new BadRequestException("Token does not match user");
            }
            String newAccessToken = jwtProvider.generateToken(account);
            log.info("Refreshed access token successfully for user: {}", username);

            return toResponse(newAccessToken, cleanToken);

        } catch (ExpiredJwtException e) {
            log.warn("Refresh token expired");
            throw new BadRequestException("Refresh token expired");
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new BadRequestException("Failed to refresh token");
        }
    }

    //========== PRIVATE METHOD =========//
    private void validateField(RegisterRequest request) {
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }
    }

    private User buildProfileUser(RegisterRequest request, Account savedAccount) {
        return User.builder()
                .account(savedAccount)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    private AccountHasRole buildAccountRole(Account account, Role userRole) {
        return AccountHasRole.builder()
                .account(account)
                .role(userRole)
                .build();
    }

    private Account buildAccount(RegisterRequest request) {
        return Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
    }

    private Account authenticationAndValidateAccount(LoginRequest request) {
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
        return account;
    }
}


        