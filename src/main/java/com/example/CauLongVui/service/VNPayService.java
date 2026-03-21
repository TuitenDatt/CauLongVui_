package com.example.CauLongVui.service;

import jakarta.servlet.http.HttpServletRequest;

public interface VNPayService {
    String createPaymentUrl(long amount, String orderInfo, String orderId, String ipAddress);
    int orderReturn(HttpServletRequest request);
    String ipnProcess(HttpServletRequest request);
}
