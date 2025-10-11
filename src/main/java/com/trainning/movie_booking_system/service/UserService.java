package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.UserUpdateRequest;
import com.trainning.movie_booking_system.dto.response.UserResponse;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.entity.User;
import com.trainning.movie_booking_system.repository.UserRepository;
import com.trainning.movie_booking_system.untils.enums.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public User createUser(Account account, String fullName, String phone, LocalDate dateOfBirth, 
                          Gender gender, String address, String city) {
        User user = User.builder()
                .account(account)
                .fullName(fullName)
                .phone(phone)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .address(address)
                .city(city)
                .build();
        
        return userRepository.save(user);
    }

    public Optional<User> getUserByAccountId(Long accountId) {
        return userRepository.findByAccountId(accountId);
    }

    public UserResponse updateUser(Long accountId, UserUpdateRequest request) {
        User user = getUserByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update user information
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }

    public UserResponse getUserResponse(Long accountId) {
        User user = getUserByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponse.fromUser(user);
    }
}
