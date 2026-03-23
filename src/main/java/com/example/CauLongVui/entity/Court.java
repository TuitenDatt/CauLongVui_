package com.example.CauLongVui.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private Double pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourtStatus status;

    public enum CourtStatus {
        AVAILABLE, BOOKED, MAINTENANCE
    }
}
