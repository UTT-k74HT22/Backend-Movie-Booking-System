package com.trainning.movie_booking_system.dto.response;

import com.trainning.movie_booking_system.entity.User;
import com.trainning.movie_booking_system.untils.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private Long accountId;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatarUrl;
    private String address;
    private String city;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .accountId(user.getAccount().getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .address(user.getAddress())
                .city(user.getCity())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
