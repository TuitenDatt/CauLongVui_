package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rackets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Racket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RacketType racketType;

    @org.hibernate.annotations.Nationalized
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double rentalPrice;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(length = 500)
    private String imageUrl;

    public enum RacketType {
        BASIC, INTERMEDIATE, ADVANCED
    }
}
