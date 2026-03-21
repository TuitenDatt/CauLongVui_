package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.MomoPaymentResponse;

public interface MomoService {
    MomoPaymentResponse createPayment(long amount, String orderId, String orderInfo) throws Exception;
}
