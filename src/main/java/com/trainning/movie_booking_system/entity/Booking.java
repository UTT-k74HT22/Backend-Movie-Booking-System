package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_account", columnList = "account_id"),
                @Index(name = "idx_booking_showtime", columnList = "showtime_id"),
//                @Index(name = "idx_booking_voucher", columnList = "voucher_id"),
                @Index(name = "idx_booking_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "voucher_id")
//    private Voucher voucher;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
    }
}
