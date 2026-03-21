package com.example.CauLongVui.controller;

import com.example.CauLongVui.config.VNPayConfig;
import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;

    /**
     * POST /api/payment/vnpay
     * Body: { bookingId, amount, courtName, customerName }
     * Tra ve { payUrl } de frontend redirect sang VNPay
     */
    @PostMapping("/vnpay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createVNPayPayment(
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long bookingId    = Long.valueOf(body.get("bookingId").toString());
            long amount       = Long.parseLong(body.get("amount").toString());
            String courtName  = body.getOrDefault("courtName",  "Dat san cau long").toString();
            String custName   = body.getOrDefault("customerName", "Khach hang").toString();

            String orderId  = "CLV-" + bookingId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String orderInfo = "Dat san " + courtName + " - " + custName;
            
            String ipAddress = VNPayConfig.getIpAddress(request);
            
            String payUrl = vnPayService.createPaymentUrl(amount, orderInfo, orderId, ipAddress);

            log.info("VNPay payment created: orderId={}, payUrl={}", orderId, payUrl);
            return ResponseEntity.ok(ApiResponse.success(Map.of("payUrl", payUrl, "orderId", orderId)));
        } catch (Exception e) {
            log.error("VNPay exception: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Loi ket noi VNPay: " + e.getMessage()));
        }
    }

    /**
     * GET /api/payment/vnpay-return
     * VNPay redirect ve sau khi user thanh toan (hoac huy)tren cong VNPay
     */
    @GetMapping("/vnpay-return")
    public RedirectView vnpayReturn(HttpServletRequest request) {
        int paymentStatus = vnPayService.orderReturn(request);

        String orderId = request.getParameter("vnp_TxnRef");
        String amount = request.getParameter("vnp_Amount");
        String message = "";
        
        boolean success = false;
        if (paymentStatus == 1) {
            success = true;
            message = "Thanh toán thành công";
        } else if (paymentStatus == 0) {
            message = "Thanh toán thất bại";
        } else {
            message = "Sai chữ ký xác thực";
        }

        log.info("VNPay return: orderId={}, status={}, message={}", orderId, paymentStatus, message);

        long amountVal = 0;
        try {
            amountVal = Long.parseLong(amount) / 100; // VNPay returns amount * 100
        } catch(Exception e) {
        }
        
        String redirectUrl = "/payment.html?result=" + (success ? "success" : "failed")
                + "&orderId=" + (orderId != null ? orderId : "")
                + "&amount=" + amountVal
                + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        return new RedirectView(redirectUrl);
    }

    /**
     * GET /api/payment/vnpay-ipn
     * IPN tu VNPay server (server-to-server) 
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        log.info("[VNPAY IPN] Received IPN Call");
        String responseFormat = vnPayService.ipnProcess(request);
        return ResponseEntity.ok(responseFormat);
    }
}
