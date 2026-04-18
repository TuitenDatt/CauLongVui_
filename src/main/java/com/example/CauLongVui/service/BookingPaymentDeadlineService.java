package com.example.CauLongVui.service;

import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động hủy booking đặt trước (BOOK_NOW_PAY_LATER) khi quá hạn thanh toán.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPaymentDeadlineService {

    private final BookingRepository bookingRepository;

    /**
     * Chạy mỗi 5 phút. Hủy các booking có paymentDeadline < now và paidAt IS NULL.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void cancelOverdueBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> overdueBookings = bookingRepository
                .findByPaymentDeadlineBeforeAndPaidAtIsNullAndStatusNot(
                        now, Booking.BookingStatus.CANCELLED);

        if (overdueBookings.isEmpty()) return;

        for (Booking booking : overdueBookings) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            log.info("Auto-cancelled overdue booking #{} (deadline was {})",
                    booking.getId(), booking.getPaymentDeadline());
        }
        bookingRepository.saveAll(overdueBookings);
        log.info("Auto-cancelled {} overdue booking(s)", overdueBookings.size());
    }
}
