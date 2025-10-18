package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends JpaRepository<Screen,Long> {

    /**
     * Check if a screen exists by its name
     *
     * @param name the name of the screen
     * @return true if a screen with the given name exists, false otherwise
     */
    boolean existsScreenByName(String name);

}
