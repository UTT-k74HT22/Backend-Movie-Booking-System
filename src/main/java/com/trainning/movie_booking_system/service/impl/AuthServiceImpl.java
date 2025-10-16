package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ForgotPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ResetPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.InternalServerErrorException;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.security.JwtProvider;
import com.trainning.movie_booking_system.service.*;
import com.trainning.movie_booking_system.untils.enums.OtpType;
import com.trainning.movie_booking_system.untils.enums.RoleType;
import com.trainning.movie_booking_system.untils.enums.UserStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());
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

        try {
            // B1: Xác thực tài khoản
            Account account = authenticationAndValidateAccount(request);
            log.info("Account {} authenticated successfully", account.getUsername());

            // B2: Sinh JWT token
            String accessToken;
            String refreshToken;
            try {
                accessToken = jwtProvider.generateToken(account);
                refreshToken = jwtProvider.generateRefreshToken(account);
                log.debug("Tokens generated for user: {}", account.getUsername());
            } catch (Exception e) {
                log.error("Error generating tokens for {}: {}", account.getUsername(), e.getMessage(), e);
                throw new InternalServerErrorException("Cannot generate JWT tokens");
            }

            // B3: Lưu refresh token vào Redis
            try {
                String key = buildRedisKey(account.getUsername());
                long ttl = (jwtProvider.getExpiration(refreshToken).getTime() - System.currentTimeMillis()) / 1000;
                redisService.set(key, refreshToken, ttl, TimeUnit.SECONDS);
                log.debug("Saved refresh token in Redis for {} with TTL={}s", account.getUsername(), ttl);
            } catch (Exception e) {
                log.error("Redis error saving refresh token for {}: {}", account.getUsername(), e.getMessage(), e);
                throw new InternalServerErrorException("Failed to store refresh token");
            }

            // B4: Trả về response
            log.info("Login successful for username: {}", account.getUsername());
            return toResponse(accessToken, refreshToken);

        } catch (BadRequestException e) {
            log.warn("Login failed (BadRequest) for {}: {}", request.getUsername(), e.getMessage());
            throw e;
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for {}: {}", request.getUsername(), e.getMessage());
            throw new BadRequestException("Invalid username or password");
        } catch (Exception e) {
            log.error("Unexpected error during login for {}: {}", request.getUsername(), e.getMessage(), e);
            throw new InternalServerErrorException("Unexpected error while logging in");
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Starting refresh token process");

        // Validate input
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        String cleanToken = cleanToken(refreshToken);
        log.debug("Processing refresh token, length: {}", cleanToken.length());

        // Validate token format and signature
        if (!jwtProvider.validateToken(cleanToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        try {
            String username = jwtProvider.extractUsername(cleanToken);

            log.debug("Processing refresh token for user: {}", username);

            // Find account
            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("User not found"));

            // Verify token matches stored token in Redis
            verifyStoredRefreshToken(username, cleanToken);

            // Validate token belongs to this account
            if (!jwtProvider.isTokenValidForAccount(cleanToken, account)) {
                log.warn("Token mismatch for user: {}", username);
                throw new BadRequestException("Token does not match user");
            }

            // Generate new access token
            String newAccessToken = jwtProvider.generateToken(account);
            log.info("Access token refreshed successfully for user: {}", username);

            return toResponse(newAccessToken, cleanToken);

        } catch (ExpiredJwtException e) {
            log.warn("Refresh token has expired");
            throw new BadRequestException("Refresh token expired");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new BadRequestException("Failed to refresh token");
        }
    }

    /**
     * Clean and normalize token string
     */
    private String cleanToken(String token) {
        if (token == null) return "";
        String cleaned = token.trim();
        // Remove leading and trailing quotes
        cleaned = cleaned.replaceAll("^\"|\"$", "");
        return cleaned;
    }

    /**
     * Verify stored refresh token in Redis matches the provided token
     */
    private void verifyStoredRefreshToken(String username, String token) {
        String redisKey = buildRedisKey(username);

        Object storedTokenObj = redisService.get(redisKey);
        if (storedTokenObj == null) {
            log.warn("Refresh token not found in Redis for user: {}", username);
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String storedToken = storedTokenObj.toString().trim();
        if (storedToken.isEmpty()) {
            log.warn("Stored refresh token is empty for user: {}", username);
            throw new BadRequestException("Invalid or expired refresh token");
        }
        storedToken = storedToken.replaceAll("^\"|\"$", "");
        if (!storedToken.equals(token)) {
            log.warn("Refresh token mismatch for user: {}. Provided token does not match stored token", username);
            throw new BadRequestException("Invalid or expired refresh token");
        }
    }

    /**
     * Build Redis key with namespace to avoid collisions
     */
    private String buildRedisKey(String username) {
        return "auth:refreshToken:" + username;
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

        if (!account.isEmailVerified()) {
            throw new BadRequestException("Email is not verified. Please verify your email first.");
        }

        if (!UserStatus.ACTIVE.equals(account.getStatus())) {
            throw new BadRequestException("Account is not active");
        }

        log.info("Authentication successful for username: {}", account.getUsername());
        return account;
    }

    public class TokenConstants {
        public static final String REDIS_REFRESH_TOKEN_PREFIX = "REFRESH_TOKEN:";
    }

    @Override
    public void logout(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new BadRequestException("Refresh token is required");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        try {
            String username = jwtProvider.extractUsername(refreshToken);

            if (StringUtils.isBlank(username)) {
                throw new BadRequestException("Failed to extract username from token");
            }

            String redisKey = buildRedisKey(username);
            redisService.delete(redisKey);

            log.info("Logout successful for user: {}", username);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to logout", e);
            throw new BadRequestException("Logout operation failed");
        }
    }


    /**
     *FORGOT PASSWORD:
     * 1. Validate email
     * 2. Find account
     * 3. Check account ACTIVE
     * 4. Check email VERIFIED
     * 5. Send OTP
     * 6. Response: "OTP sent to email"
     *
     * */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("forgotPassword for email: {}", request.getEmail());

        //check validate email
        if (StringUtils.isBlank(request.getEmail())){
            throw new BadRequestException("Email is required");
        }
        //find account by email
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Account not found for email: {}", request.getEmail());
                    return new BadRequestException("Email not found in system");
                });
        // check if email is verified and account is active
        if (!UserStatus.ACTIVE.equals(account.getStatus())) {
            log.warn("Inactive account trying forgot password: {}", account.getUsername());
            throw new BadRequestException("Account is not active");
        }
        //send otp to email
        try {
            otpService.sendOtp(account.getEmail(), OtpType.FORGOT_PASSWORD);
            log.info("Forgot password OTP sent to email: {}", account.getEmail());
        }catch (Exception e) {
            log.error("Failed to send forgot password OTP", e);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }

    }
    /**  * RESET PASSWORD:
     * 1. Validations (email, otp, passwords)
     * 2. Verify OTP
     * 3. Find account
     * 4. Update password (encode)
     * 5. Delete OTP ← Cleanup
     * 6. Delete refresh tokens ← Force re-login
     * 7. Response: "Password reset success"*/
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        // ========== VALIDATIONS ==========

        if (StringUtils.isBlank(request.getEmail())) {
            throw new BadRequestException("Email is required");
        }

        if (StringUtils.isBlank(request.getOtp())) {
            throw new BadRequestException("OTP is required");
        }

        if (StringUtils.isBlank(request.getNewPassword())) {
            throw new BadRequestException("New password is required");
        }

        if (StringUtils.isBlank(request.getConfirmPassword())) {
            throw new BadRequestException("Confirm password is required");
        }

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password mismatch for email: {}", request.getEmail());
            throw new BadRequestException("Passwords do not match");
        }
        // ========== VERIFY OTP ==========

        boolean isValidOtp = otpService.verifyOtp(
                request.getEmail(),
                request.getOtp(),
                OtpType.FORGOT_PASSWORD
        );

        if (!isValidOtp) {
            log.warn("Invalid OTP for email: {}", request.getEmail());
            throw new BadRequestException("Invalid or expired OTP");
        }

        // ========== UPDATE PASSWORD ==========

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Account not found during password reset: {}", request.getEmail());
                    return new BadRequestException("Account not found");
                });
        // Encode and update password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        account.setPassword(encodedPassword);
        accountRepository.save(account);

        log.info("Password updated successfully for account: {}", account.getId());

        // ========== CLEANUP & INVALIDATE TOKENS ==========

        //  Delete OTP from Redis
        try {
            otpService.deleteOtp(request.getEmail(), OtpType.FORGOT_PASSWORD);
            log.debug("OTP deleted from Redis for email: {}", request.getEmail());
        } catch (Exception e) {
            log.warn("Failed to delete OTP, but password was reset successfully", e);
        }

        // Invalidate all refresh tokens (force user to login again)
        String refreshTokenRedisKey = buildRedisKey(account.getUsername());
        try {
            redisService.delete(refreshTokenRedisKey);
            log.debug("Refresh tokens invalidated for user: {}", account.getUsername());
        } catch (Exception e) {
            log.warn("Failed to invalidate refresh tokens, but password was reset successfully", e);
        }

        log.info("Password reset completed successfully for user: {}", account.getUsername());
    }
    }