package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String customerPhone;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column
    private Double totalPrice;

    @Builder.Default
    @Column(name = "is_pass")
    private Boolean isPass = false;

    @Column
    private Double passPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 120)
    private String paymentReference;

    @Column
    private LocalDateTime paidAt;

    @Column
    private LocalDateTime refundedAt;

    /** Deadline tra tien cho booking dat truoc (PRO/VIP). Null = da tra hoac khong ap dung. */
    @Column
    private LocalDateTime paymentDeadline;

    /** Cụm đặt sân (nullable). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id")
    private BookingBundle bundle;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

    public enum PaymentMethod {
        CASH, MOMO, WALLET, BOOK_NOW_PAY_LATER
    }
}
