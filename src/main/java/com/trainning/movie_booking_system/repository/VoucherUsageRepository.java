package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.VoucherUsage;
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
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    /**
     * Count how many times a user has used a specific voucher
     */
    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId AND vu.user.id = :userId")
    long countByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") Long userId);

    /**
     * Find all voucher usages by user ID
     */
    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.user.id = :userId ORDER BY vu.usedAt DESC")
    Page<VoucherUsage> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find all usages for a specific voucher
     */
    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId ORDER BY vu.usedAt DESC")
    Page<VoucherUsage> findByVoucherId(@Param("voucherId") Long voucherId, Pageable pageable);

    /**
     * Find voucher usage by booking ID
     */
    Optional<VoucherUsage> findByBookingId(Long bookingId);

    /**
     * Find usages within a date range (for analytics)
     */
    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.usedAt BETWEEN :startDate AND :endDate")
    List<VoucherUsage> findUsagesBetweenDates(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Get total discount amount for a voucher (analytics)
     */
    @Query("SELECT COALESCE(SUM(vu.discountAmount), 0) FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId")
    Long getTotalDiscountByVoucher(@Param("voucherId") Long voucherId);

    /**
     * Delete voucher usage by booking ID (for refund/cancellation)
     */
    void deleteByBookingId(Long bookingId);

    /**
     * Check if booking already used a voucher
     */
    boolean existsByBookingId(Long bookingId);
}
