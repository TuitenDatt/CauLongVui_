package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 150)
    private String name;

    @org.hibernate.annotations.Nationalized
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(length = 500)
    private String imageUrl;

    @org.hibernate.annotations.Nationalized
    @Column(length = 100)
    private String category;
}
