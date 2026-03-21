package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.MomoPaymentResponse;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.service.BookingService;
import com.example.CauLongVui.service.MomoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class MomoController {

    private final MomoService momoService;
    private final BookingService bookingService;

    /**
     * POST /api/payment/momo
     * Body: { bookingId, amount, courtName, customerName }
     * Tra ve { payUrl } de frontend redirect sang MoMo
     */
    @PostMapping("/momo")
    public ResponseEntity<ApiResponse<Map<String, String>>> createMomoPayment(
            @RequestBody Map<String, Object> body) {
        try {
            Long bookingId    = Long.valueOf(body.get("bookingId").toString());
            long amount       = Long.parseLong(body.get("amount").toString());
            String courtName  = body.getOrDefault("courtName",  "Dat san cau long").toString();
            String custName   = body.getOrDefault("customerName", "Khach hang").toString();

            String orderId  = "CLV-" + bookingId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String orderInfo = "Dat san " + courtName + " - " + custName;

            MomoPaymentResponse resp = momoService.createPayment(amount, orderId, orderInfo);

            if (resp.getResultCode() != null && resp.getResultCode() == 0 && resp.getPayUrl() != null) {
                log.info("MoMo payment created: orderId={}, payUrl={}", orderId, resp.getPayUrl());
                return ResponseEntity.ok(ApiResponse.success(Map.of("payUrl", resp.getPayUrl(), "orderId", orderId)));
            } else {
                log.warn("MoMo error: code={}, msg={}", resp.getResultCode(), resp.getMessage());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Khong the tao thanh toan MoMo: " + resp.getMessage()));
            }
        } catch (Exception e) {
            log.error("MoMo exception: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Loi ket noi MoMo: " + e.getMessage()));
        }
    }

    /**
     * GET /api/payment/momo-return
     * MoMo redirect ve sau khi user quet QR (hoac huy)
     * Redirect sang payment.html voi ket qua
     */
    @GetMapping("/momo-return")
    public org.springframework.web.servlet.view.RedirectView momoReturn(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Long amount,
            @RequestParam(required = false) String orderInfo,
            @RequestParam(required = false) Integer resultCode,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) Long transId) {

        boolean success = resultCode != null && resultCode == 0;
        log.info("MoMo return: orderId={}, resultCode={}, transId={}", orderId, resultCode, transId);

        // Cap nhat trang thai booking neu co bookingId trong orderId (CLV-{id}-xxx)
        if (orderId != null && orderId.startsWith("CLV-")) {
            try {
                String[] parts = orderId.split("-");
                if (parts.length >= 2) {
                    Long bookingId = Long.parseLong(parts[1]);
                    Booking.BookingStatus newStatus = success
                            ? Booking.BookingStatus.CONFIRMED
                            : Booking.BookingStatus.CANCELLED;
                    bookingService.updateBookingStatus(bookingId, newStatus);
                    log.info("Booking {} updated to {}", bookingId, newStatus);
                }
            } catch (Exception e) {
                log.warn("Could not update booking status: {}", e.getMessage());
            }
        }

        String redirectUrl = "/payment.html?result=" + (success ? "success" : "failed")
                + "&orderId=" + (orderId != null ? orderId : "")
                + "&amount=" + (amount != null ? amount : 0)
                + "&transId=" + (transId != null ? transId : "")
                + "&message=" + java.net.URLEncoder.encode(message != null ? message : "", java.nio.charset.StandardCharsets.UTF_8);

        return new org.springframework.web.servlet.view.RedirectView(redirectUrl);
    }

    /**
     * POST /api/payment/momo-notify
     * IPN tu MoMo server (server-to-server)
     */
    @PostMapping("/momo-notify")
    public ResponseEntity<String> momoNotify(@RequestBody String body) {
        log.info("[MOMO IPN] {}", body);
        return ResponseEntity.ok("OK");
    }
}
