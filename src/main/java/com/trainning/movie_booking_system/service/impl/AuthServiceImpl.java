package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.security.JwtProvider;
import com.trainning.movie_booking_system.service.AuthService;
import com.trainning.movie_booking_system.service.MailService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


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
    private final MailService mailService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("Starting registration for username: {}", request.getUsername());

        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
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

        // Generate 6-digit OTP and expiry (e.g., 10 minutes)
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        log.info("[DEV] Generated OTP value for {} is {}", savedAccount.getEmail(), otp);
        savedAccount.setOtpCode(otp);
        savedAccount.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        savedAccount.setLastOtpSentAt(LocalDateTime.now());
        accountRepository.save(savedAccount);
        log.info("Generated OTP for {}", savedAccount.getEmail());

        // Send OTP về email đăng kí

        mailService.sendSimpleEmailAsync(
                savedAccount.getEmail(),
                "Your verification OTP",
                "Your OTP is: " + otp + " (valid for 10 minutes)"
        );
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
    /*
    * khi email dc gửi về -> email dc verify hay ko bằng mã otp 
    *
    * */
    @Override
    @Transactional
    public void verifyOtp(String email, String otp) {
        String providedOtp = otp != null ? otp.trim() : "";
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email not found"));

        if (account.isEmailVerified()) {
            return;
        }

        if (account.getOtpCode() == null || account.getOtpExpiresAt() == null) {
            throw new BadRequestException("OTP not requested");
        }

        if (account.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired");
        }

        log.info("[DEV] Verifying OTP: provided='{}', expected='{}', expiresAt='{}'", providedOtp, account.getOtpCode(), account.getOtpExpiresAt());

        if (!account.getOtpCode().equals(providedOtp)) {
            throw new BadRequestException("Invalid OTP");
        }

        account.setEmailVerified(true);
        account.setOtpCode(null);
        account.setOtpExpiresAt(null);
        accountRepository.save(account);
        log.info("Email verified via OTP for account: {}", account.getUsername());
    }


    /*
     *   Thực hiện gửi lại mã otp trong trường hợp mã otp hết hạn hoặc ko thấy mã otp
     * */
    @Override
    @Transactional
    public void resendOtp(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email not found"));

        if (account.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        // Kiểm tra thời gian gửi lại
        if (account.getLastOtpSentAt() != null) {
            Duration duration = Duration.between(account.getLastOtpSentAt(), LocalDateTime.now());
            if (duration.getSeconds() < 60) { // < 1 phút
                throw new BadRequestException("Please wait 1 minute before requesting another OTP");
            }
        }
        // Generate new 6-digit OTP and expiry (e.g., 10 minutes)
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        log.info("[DEV] Resent OTP value for {} is {}", account.getEmail(), otp);
        account.setOtpCode(otp);
        account.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        account.setLastOtpSentAt(LocalDateTime.now());
        accountRepository.save(account);
        log.info("Resent OTP for {}", account.getEmail());

        // Send OTP về email đăng kí
        mailService.sendSimpleEmailAsync(
                account.getEmail(),
                "Your verification OTP",
                "Your OTP is: " + otp + " (valid for 10 minutes)"
        );
    }

    public Map<String, String> refreshToken(String refreshToken) {
        Map<String, String> response = new HashMap<>();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        try {
            String username = jwtProvider.extractUsername(refreshToken);

            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("User not found"));

            if (!jwtProvider.isTokenValidForAccount(refreshToken, account)) {
                throw new BadRequestException("Token does not match user");
            }

            String newAccessToken = jwtProvider.generateToken(account);

            response.put("accessToken", newAccessToken);
            response.put("refreshToken", refreshToken); // Giữ nguyên refresh token cũ

            return response;

        } catch (ExpiredJwtException e) {
            throw new BadRequestException("Refresh token expired");
        } catch (Exception e) {
            throw new BadRequestException("Failed to refresh token");
        }
    }

}


        