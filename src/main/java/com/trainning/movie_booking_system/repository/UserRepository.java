package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Java docs
     * @param accountId
     * @return
     */
    Optional<User> findByAccountId(Long accountId);
    
    Optional<User> findByPhone(String phone);
}
