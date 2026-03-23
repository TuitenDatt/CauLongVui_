package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.Racket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RacketDTO {
    private Long id;
    private String name;
    private Racket.RacketType racketType;
    private String description;
    private Double rentalPrice;
    private Integer stockQuantity;
    private String imageUrl;
}
