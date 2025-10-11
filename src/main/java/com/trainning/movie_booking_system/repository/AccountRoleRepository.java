package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.AccountHasRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRoleRepository extends JpaRepository<AccountHasRole, Long> {
    
    List<AccountHasRole> findByAccountId(Long accountId);
    
    List<AccountHasRole> findByRoleId(Long roleId);
    
    Optional<AccountHasRole> findByAccountIdAndRoleId(Long accountId, Long roleId);
    
    boolean existsByAccountIdAndRoleId(Long accountId, Long roleId);
    
    void deleteByAccountIdAndRoleId(Long accountId, Long roleId);
}
