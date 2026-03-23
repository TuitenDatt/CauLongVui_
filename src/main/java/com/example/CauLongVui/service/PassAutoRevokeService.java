package com.example.CauLongVui.service;

import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassAutoRevokeService {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) // Run every 1 minute
    @Transactional
    public void autoRevokeExpiredPasses() {
        List<Booking> passedBookings = bookingRepository.findByIsPassTrueOrderByBookingDateDesc();
        if (passedBookings.isEmpty()) return;

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        int count = 0;
        for (Booking b : passedBookings) {
            boolean isExpired = false;
            if (b.getBookingDate() != null) {
                if (b.getBookingDate().isBefore(today)) {
                    isExpired = true;
                } else if (b.getBookingDate().isEqual(today)) {
                    if (b.getEndTime() != null && b.getEndTime().isBefore(now)) {
                        isExpired = true;
                    }
                }
            }
            if (isExpired) {
                b.setIsPass(false);
                b.setPassPrice(null);
                bookingRepository.save(b);
                count++;
                log.info("Auto-revoked expired pass for booking ID: {}", b.getId());
            }
        }
        
        if (count > 0) {
            log.info("Total auto-revoked passes: {}", count);
        }
    }
}
