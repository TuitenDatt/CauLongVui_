package com.example.CauLongVui.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderRequest {
    private Long courtId;
    private Long bookingId;
    private String deliveryTime; // format "HH:mm"
    private String customerName;
    private List<OrderItemRequest> items;
}
