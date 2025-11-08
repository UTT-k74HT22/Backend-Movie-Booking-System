package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Voucher;
import com.trainning.movie_booking_system.untils.enums.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /**
     * Find voucher by code (case-insensitive)
     */
    Optional<Voucher> findByCodeIgnoreCase(String code);

    /**
     * Check if voucher code exists
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Find all active public vouchers
     */
    @Query("SELECT v FROM Voucher v WHERE v.status = :status AND v.isPublic = true " +
           "AND v.validFrom <= :now AND v.validUntil >= :now " +
           "AND v.currentUsageCount < v.totalUsageLimit")
    Page<Voucher> findActivePublicVouchers(@Param("status") VoucherStatus status,
                                           @Param("now") LocalDateTime now,
                                           Pageable pageable);

    /**
     * Find vouchers by status
     */
    List<Voucher> findByStatus(VoucherStatus status);

    /**
     * Find expired vouchers that need status update
     */
    @Query("SELECT v FROM Voucher v WHERE v.status = :activeStatus AND v.validUntil < :now")
    List<Voucher> findExpiredVouchers(@Param("activeStatus") VoucherStatus activeStatus,
                                     @Param("now") LocalDateTime now);

    /**
     * Find vouchers with usage limit reached
     */
    @Query("SELECT v FROM Voucher v WHERE v.status = :status AND v.currentUsageCount >= v.totalUsageLimit")
    List<Voucher> findVouchersWithUsageLimitReached(@Param("status") VoucherStatus status);

    /**
     * Find all vouchers created by a specific user (admin)
     */
    @Query("SELECT v FROM Voucher v WHERE v.createdBy = :userId ORDER BY v.createdAt DESC")
    Page<Voucher> findByCreatedBy(@Param("userId") Long userId, Pageable pageable);
}
