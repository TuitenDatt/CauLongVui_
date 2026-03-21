package com.example.CauLongVui.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MomoPaymentResponse {
    private String partnerCode;
    private String requestId;
    private String orderId;
    private Long amount;
    private Long responseTime;
    private String message;
    private Integer resultCode;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
}
