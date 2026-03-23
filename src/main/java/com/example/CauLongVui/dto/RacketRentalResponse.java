package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.RacketRentalOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RacketRentalResponse {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String courtName;
    private String cccdImageUrl;
    private Double totalAmount;
    private RacketRentalOrder.OrderStatus status;
    private RacketRentalOrder.PaymentMethod paymentMethod;
    private LocalDateTime orderDate;
    private List<RentalItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RentalItemResponse {
        private Long racketId;
        private String racketName;
        private Integer quantity;
        private Double unitPrice;
        private String imageUrl;
    }
}
