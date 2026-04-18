package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.BookingBundleDTO;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.service.BookingBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bundles")
@RequiredArgsConstructor
public class BookingBundleController {

    private final BookingBundleService bundleService;

    /**
     * POST /api/bundles
     * Body JSON:
     * {
     *   "userId": 1, "bundleName": "Cty ABC", "note": "...",
     *   "bookingDate": "2026-04-25", "startTime": "07:00", "endTime": "09:00",
     *   "courtIds": [1, 2, 3],
     *   "paymentMethod": "WALLET",
     *   "customerName": "Nguyen Van A", "customerPhone": "0901234567"
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingBundleDTO>> createBundle(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String bundleName = body.containsKey("bundleName") ? body.get("bundleName").toString() : null;
        String note = body.containsKey("note") ? body.get("note").toString() : null;
        LocalDate bookingDate = LocalDate.parse(body.get("bookingDate").toString());
        LocalTime startTime = LocalTime.parse(body.get("startTime").toString());
        LocalTime endTime = LocalTime.parse(body.get("endTime").toString());
        @SuppressWarnings("unchecked")
        List<Long> courtIds = ((List<?>) body.get("courtIds"))
                .stream().map(o -> Long.valueOf(o.toString())).toList();
        Booking.PaymentMethod method = Booking.PaymentMethod.valueOf(
                body.getOrDefault("paymentMethod", "MOMO").toString());
        String customerName = body.get("customerName").toString();
        String customerPhone = body.get("customerPhone").toString();

        BookingBundleDTO result = bundleService.createBundle(
                userId, bundleName, note, bookingDate, startTime, endTime,
                courtIds, method, customerName, customerPhone);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao cum dat san thanh cong!", result));
    }

    /** GET /api/bundles?userId= */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingBundleDTO>>> getBundles(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(bundleService.getBundlesByUserId(userId)));
    }

    /** GET /api/bundles/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingBundleDTO>> getBundle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bundleService.getBundleById(id)));
    }

    /** POST /api/bundles/{id}/pay-wallet?userId= */
    @PostMapping("/{id}/pay-wallet")
    public ResponseEntity<ApiResponse<BookingBundleDTO>> payWithWallet(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Thanh toan bang vi thanh cong!", bundleService.payBundleWithWallet(id, userId)));
    }

    /** POST /api/bundles/{id}/confirm-payment */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingBundleDTO>> confirmPayment(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MOMO") Booking.PaymentMethod method,
            @RequestParam(required = false) String paymentReference) {
        return ResponseEntity.ok(ApiResponse.success(
                "Xac nhan thanh toan thanh cong!", bundleService.confirmBundlePayment(id, method, paymentReference)));
    }

    /** POST /api/bundles/{id}/sell-one?bookingId=&userId= */
    @PostMapping("/{id}/sell-one")
    public ResponseEntity<ApiResponse<BookingBundleDTO>> sellOne(
            @PathVariable Long id,
            @RequestParam Long bookingId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da dang ban san thanh cong!", bundleService.sellOneBooking(id, bookingId, userId)));
    }

    /** POST /api/bundles/{id}/sell-all?userId= */
    @PostMapping("/{id}/sell-all")
    public ResponseEntity<ApiResponse<BookingBundleDTO>> sellAll(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da dang ban het cum san!", bundleService.sellAllBookings(id, userId)));
    }

    /** DELETE /api/bundles/{id}?userId= */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingBundleDTO>> cancelBundle(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da huy cum dat san!", bundleService.cancelBundle(id, userId)));
    }
}
