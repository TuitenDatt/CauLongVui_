package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.MomoPaymentResponse;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.service.BookingService;
import com.example.CauLongVui.service.MomoService;
import com.example.CauLongVui.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class MomoController {

    private final MomoService momoService;
    private final BookingService bookingService;
    private final com.example.CauLongVui.service.MembershipService membershipService;
    private final WalletService walletService;
    private final com.example.CauLongVui.service.BookingBundleService bundleService;

    @PostMapping("/momo")
    public ResponseEntity<ApiResponse<Map<String, String>>> createMomoPayment(
            @RequestBody Map<String, Object> body
    ) {
        try {
            Long bookingId = Long.valueOf(body.getOrDefault("bookingId", 0).toString());
            long amount = Long.parseLong(body.get("amount").toString());
            String courtName = body.getOrDefault("courtName", "Dat san cau long").toString();
            String customerName = body.getOrDefault("customerName", "Khach hang").toString();
            String customOrderId = body.getOrDefault("orderId", "").toString();
            boolean isBundle = Boolean.TRUE.equals(body.get("isBundlePayment"));

            String orderId = (customOrderId != null && !customOrderId.isBlank())
                    ? customOrderId
                    : (isBundle ? "BUNDLE-" : "CLV-") + bookingId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String orderInfo = (isBundle ? "Dat cum san " : "Dat san ") + courtName + " - " + customerName;

            MomoPaymentResponse response = momoService.createPayment(amount, orderId, orderInfo);

            if (response.getResultCode() != null && response.getResultCode() == 0 && response.getPayUrl() != null) {
                log.info("MoMo payment created: orderId={}, payUrl={}", orderId, response.getPayUrl());
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                        "payUrl", response.getPayUrl(),
                        "orderId", orderId
                )));
            }

            log.warn("MoMo error: code={}, msg={}", response.getResultCode(), response.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Khong the tao thanh toan MoMo: " + response.getMessage()));
        } catch (Exception e) {
            log.error("MoMo exception: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Loi ket noi MoMo: " + e.getMessage()));
        }
    }

    @GetMapping("/momo-return")
    public org.springframework.web.servlet.view.RedirectView momoReturn(
            @RequestParam(name = "orderId", required = false) String orderId,
            @RequestParam(name = "amount", required = false) Long amount,
            @RequestParam(name = "orderInfo", required = false) String orderInfo,
            @RequestParam(name = "resultCode", required = false) Integer resultCode,
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "transId", required = false) Long transId
    ) {
        boolean success = resultCode != null && resultCode == 0;
        String flow = "payment";
        log.info("MoMo return: orderId={}, resultCode={}, transId={}", orderId, resultCode, transId);

        if (orderId != null && orderId.startsWith("CLV-")) {
            try {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    Long bookingId = Long.parseLong(parts[1]);
                    if (success) {
                        bookingService.markBookingPaid(bookingId, Booking.PaymentMethod.MOMO, orderId);
                    } else {
                        bookingService.markBookingPaymentFailed(bookingId);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not update booking status: {}", e.getMessage());
            }
            flow = "booking";
        } else if (orderId != null && orderId.startsWith("MB-")) {
            try {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    Long subscriptionId = Long.parseLong(parts[1]);
                    if (success) {
                        membershipService.completePurchase(subscriptionId);
                    } else {
                        membershipService.cancelPurchase(subscriptionId);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not update membership status: {}", e.getMessage());
            }
            flow = "membership";
        } else if (orderId != null && orderId.startsWith("TOPUP_")) {
            try {
                if (success) {
                    walletService.completeTopUp(orderId, transId != null ? transId.toString() : null);
                } else {
                    walletService.failTopUp(orderId, message);
                }
            } catch (Exception e) {
                log.warn("Could not update wallet topup status: {}", e.getMessage());
            }
            flow = "wallet-topup";
        } else if (orderId != null && orderId.startsWith("BUNDLE-")) {
            try {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    Long bundleId = Long.parseLong(parts[1]);
                    if (success) {
                        bundleService.confirmBundlePayment(bundleId, Booking.PaymentMethod.MOMO, orderId);
                    }
                    // Bundle doesn't have a "failed" status update logic in service yet, 
                    // usually they just stay PENDING or get auto-deleted if needed.
                }
            } catch (Exception e) {
                log.warn("Could not update bundle status: {}", e.getMessage());
            }
            flow = "bundle";
        }

        String redirectUrl = "/payment.html?result=" + (success ? "success" : "failed")
                + "&flow=" + flow
                + "&orderId=" + (orderId != null ? orderId : "")
                + "&amount=" + (amount != null ? amount : 0)
                + "&transId=" + (transId != null ? transId : "")
                + "&message=" + java.net.URLEncoder.encode(message != null ? message : "", StandardCharsets.UTF_8);

        return new org.springframework.web.servlet.view.RedirectView(redirectUrl);
    }

    @PostMapping("/momo-notify")
    public ResponseEntity<String> momoNotify(@RequestBody Map<String, Object> body) {
        log.info("[MOMO IPN] Received IPN Call: {}", body);

        try {
            String orderId = body.get("orderId").toString();
            int resultCode = Integer.parseInt(body.get("resultCode").toString());
            boolean success = resultCode == 0;

            if (orderId.startsWith("CLV-")) {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    Long bookingId = Long.parseLong(parts[1]);
                    if (success) {
                        bookingService.markBookingPaid(bookingId, Booking.PaymentMethod.MOMO, orderId);
                    } else {
                        bookingService.markBookingPaymentFailed(bookingId);
                    }
                }
            } else if (orderId.startsWith("MB-") && success) {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    membershipService.completePurchase(Long.parseLong(parts[1]));
                }
            } else if (orderId.startsWith("TOPUP_")) {
                if (success) {
                    walletService.completeTopUp(orderId, body.getOrDefault("transId", "").toString());
                } else {
                    walletService.failTopUp(orderId, body.getOrDefault("message", "").toString());
                }
            } else if (orderId.startsWith("BUNDLE-")) {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    Long bundleId = Long.parseLong(parts[1]);
                    if (success) {
                        bundleService.confirmBundlePayment(bundleId, Booking.PaymentMethod.MOMO, orderId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing MoMo IPN: {}", e.getMessage());
        }

        return ResponseEntity.ok("OK");
    }
}
