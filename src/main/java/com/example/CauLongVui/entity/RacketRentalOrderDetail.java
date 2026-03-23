package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "racket_rental_order_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RacketRentalOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_order_id", nullable = false)
    private RacketRentalOrder rentalOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "racket_id", nullable = false)
    private Racket racket;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;
}
