package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.BookingDTO;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // GET /api/bookings/slots?courtId=&date= — lấy các slot đã đặt theo sân và ngày (dùng cho lịch)
    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBookedSlots(
            @RequestParam(name = "courtId") Long courtId,
            @RequestParam(name = "date") String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Map<String, String>> slots = bookingService.getBookedSlots(courtId, localDate);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    // GET /api/bookings — lấy tất cả đặt sân (có thể filter theo courtId hoặc phone)
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getAllBookings(
            @RequestParam(name = "courtId", required = false) Long courtId,
            @RequestParam(name = "userId",  required = false) Long userId,
            @RequestParam(name = "phone",   required = false) String phone,
            @RequestParam(name = "isPass",  required = false) Boolean isPass) {
        List<BookingDTO> bookings;
        if (Boolean.TRUE.equals(isPass)) {
            bookings = bookingService.getPassBookings();
        } else if (courtId != null) {
            bookings = bookingService.getBookingsByCourtId(courtId);
        } else if (userId != null) {
            bookings = bookingService.getBookingsByUserId(userId);
        } else if (phone != null && !phone.isBlank()) {
            bookings = bookingService.getBookingsByPhone(phone);
        } else {
            bookings = bookingService.getAllBookings();
        }
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    // GET /api/bookings/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDTO>> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id)));
    }

    // POST /api/bookings — đặt sân mới (optional holdId from RSocket hold)
    @PostMapping
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
            @RequestBody BookingDTO bookingDTO,
            @RequestParam(name = "holdId", required = false) String holdId) {
        BookingDTO created = bookingService.createBooking(bookingDTO, holdId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt sân thành công", created));
    }

    // PATCH /api/bookings/{id}/status — cập nhật trạng thái
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingDTO>> updateStatus(@PathVariable Long id,
                                                                  @RequestParam(name = "status") Booking.BookingStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công",
                bookingService.updateBookingStatus(id, status)));
    }

    // PATCH /api/bookings/{id}/pass — đăng bán lại sân
    @PatchMapping("/{id}/pass")
    public ResponseEntity<ApiResponse<BookingDTO>> updateToPass(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đăng bán lại sân thành công",
                bookingService.updateBookingToPass(id)));
    }

    // POST /api/bookings/{id}/buy-pass
    @PostMapping("/{id}/buy-pass")
    public ResponseEntity<ApiResponse<BookingDTO>> buyPassBooking(
            @PathVariable Long id,
            @RequestBody BookingDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Mua lai san thanh cong",
                bookingService.buyPassBooking(id, request.getUserId(), request.getCustomerName(), request.getCustomerPhone())));
    }

    /** POST /api/bookings/{id}/pay-later — PRO/VIP thanh toan booking da dat truoc */
    @PostMapping("/{id}/pay-later")
    public ResponseEntity<ApiResponse<BookingDTO>> payLater(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "WALLET") Booking.PaymentMethod method,
            @RequestParam(required = false) String paymentReference) {
        BookingDTO result = bookingService.payDeposit(id, userId, method, paymentReference);
        return ResponseEntity.ok(ApiResponse.success("Thanh toan thanh cong!", result));
    }

    /** GET /api/bookings/unpaid?userId= — booking chua thanh toan */
    @GetMapping("/unpaid")
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getUnpaidBookings(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getUnpaidBookings(userId)));
    }
}
