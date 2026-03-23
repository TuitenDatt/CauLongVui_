package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "racket_rental_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RacketRentalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(length = 20)
    private String customerPhone;

    @org.hibernate.annotations.Nationalized
    @Column(length = 255)
    private String courtName;

    @Column(nullable = false, length = 500)
    private String cccdImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Double totalAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "booking_id")
    private Long bookingId;

    @OneToMany(mappedBy = "rentalOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RacketRentalOrderDetail> details = new ArrayList<>();

    public enum OrderStatus {
        PENDING, PAID, RETURNED, CANCELLED
    }

    public enum PaymentMethod {
        CASH, MOMO
    }
}
