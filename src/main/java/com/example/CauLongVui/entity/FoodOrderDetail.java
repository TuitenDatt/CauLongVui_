package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_order_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_order_id", nullable = false)
    private FoodOrder foodOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;
}
