package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.FoodOrder;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderResponse {
    private Long id;
    private String customerName;
    private Long courtId;
    private String courtName;
    private LocalTime deliveryTime;
    private Long bookingId;
    private Double totalAmount;
    private FoodOrder.OrderStatus status;
    private LocalDateTime orderDate;
    private List<FoodOrderDetailResponse> items;
}
