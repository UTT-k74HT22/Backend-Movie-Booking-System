package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.AccountLoginRequest;
import com.trainning.movie_booking_system.dto.request.AccountRegisterRequest;
import com.trainning.movie_booking_system.dto.response.AccountResponse;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.exception.AppException;
import com.trainning.movie_booking_system.exception.ErrorCode;
import com.trainning.movie_booking_system.repository.AccountRepository;
import com.trainning.movie_booking_system.untils.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Value("${jwt.expiryMinutes}")
    private int expiryMinutes;


    @Transactional
    public AccountResponse register(AccountRegisterRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_TAKEN);
        }
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .status(Status.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        
        // Create user profile
        userService.createUser(
            saved,
            request.getFullName(),
            request.getPhone(),
            request.getDateOfBirth(),
            request.getGender(),
            request.getAddress(),
            request.getCity()
        );
        
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse findByUsernameOrEmail(String usernameOrEmail) {
        Account account = accountRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> accountRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND)));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public Account validateLogin(AccountLoginRequest request) {
        Account account = accountRepository.findByUsername(request.getUsernameOrEmail())
                .orElseGet(() -> accountRepository.findByEmail(request.getUsernameOrEmail())
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS)));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        if (account.getStatus() == Status.DISABLED) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        return account;
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .emailVerified(account.getEmailVerified())
                .status(account.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }
}


