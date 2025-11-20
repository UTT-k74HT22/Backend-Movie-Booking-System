package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Admin.CreateUserRequest;
import com.trainning.movie_booking_system.dto.request.Admin.UpdateUserRequest;
import com.trainning.movie_booking_system.dto.response.Admin.UserAdminResponse;
import com.trainning.movie_booking_system.exception.AlreadyExistsException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.mapper.UserAdminMapper;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.service.AdminUserService;
import com.trainning.movie_booking_system.untils.enums.RoleType;
import com.trainning.movie_booking_system.untils.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ======================================================================
    // CREATE USER
    // ======================================================================
    @Override
    @Transactional
    public UserAdminResponse createUser(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        if (username.isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }
        if (password.isEmpty() || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (accountRepository.existsByEmail(email)) {
            throw new AlreadyExistsException("Email đã được sử dụng");
        }
        if (accountRepository.existsByUsername(username)) {
            throw new AlreadyExistsException("Tên đăng nhập đã được sử dụng");
        }

        // Tạo Account
        Account account = new Account();
        account.setEmail(email);
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setStatus(UserStatus.ACTIVE);
        account.setEmailVerified(true);

        // Tạo User
        User user = new User();
        user.setFirstName(request.getFirstName() != null ? request.getFirstName().trim() : null);
        user.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
        user.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null);
        user.setAccount(account);

        // Thiết lập ROLE
        RoleType roleType = Boolean.TRUE.equals(request.getIsStaff()) ? RoleType.STAFF : RoleType.USER;
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + roleType.name()));

        // Tạo mapping account-role
        AccountHasRole link = new AccountHasRole();
        link.setAccount(account);
        link.setRole(role);

        // Lưu account + user
        account = accountRepository.save(account);
        account.getAccountRoles().add(link);

        user = userRepository.save(user);

        return UserAdminMapper.toResponse(user);
    }

    // ======================================================================
    // GET USER BY ID
    // ======================================================================
    @Override
    @Transactional(readOnly = true)
    public UserAdminResponse getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return userRepository.findById(id)
                .map(UserAdminMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    // ======================================================================
    // UPDATE USER
    // ======================================================================
    @Override
    @Transactional
    public UserAdminResponse updateUser(Long id, UpdateUserRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        Account account = user.getAccount();

        // Update User info
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }

        // Update ACTIVE / INACTIVE
        if (request.getIsActive() != null) {
            account.setStatus(request.getIsActive() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        }

        // Update ROLE
        if (request.getIsStaff() != null) {
            RoleType newRoleType = request.getIsStaff() ? RoleType.STAFF : RoleType.USER;

            Role newRole = roleRepository.findByName(newRoleType)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + newRoleType.name()));

            account.getAccountRoles().clear();

            AccountHasRole link = new AccountHasRole();
            link.setAccount(account);
            link.setRole(newRole);

            account.getAccountRoles().add(link);
        }

        userRepository.save(user);
        return UserAdminMapper.toResponse(user);
    }

    // ======================================================================
    // DEACTIVATE USER
    // ======================================================================
    @Override
    @Transactional
    public void deactivateUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.getAccount().setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    // ======================================================================
    // GET ALL USERS
    // ======================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserAdminMapper::toResponse);
    }
}
