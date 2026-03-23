package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.RacketRentalOrder;
import lombok.Data;

import java.util.List;

@Data
public class RacketRentalRequest {
    private String customerName;
    private String customerPhone;
    private String courtName;
    private Long bookingId;
    private String cccdImageUrl;
    private RacketRentalOrder.PaymentMethod paymentMethod;
    private List<RentalItemRequest> items;

    @Data
    public static class RentalItemRequest {
        private Long racketId;
        private Integer quantity;
    }
}
