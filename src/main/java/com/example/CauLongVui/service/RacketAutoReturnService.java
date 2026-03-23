package com.example.CauLongVui.service;

import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.entity.Racket;
import com.example.CauLongVui.entity.RacketRentalOrder;
import com.example.CauLongVui.entity.RacketRentalOrderDetail;
import com.example.CauLongVui.repository.BookingRepository;
import com.example.CauLongVui.repository.RacketRentalOrderRepository;
import com.example.CauLongVui.repository.RacketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RacketAutoReturnService {

    private final RacketRentalOrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final RacketRepository racketRepository;

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional
    public void autoReturnExpiredRackets() {
        List<RacketRentalOrder> activeRentals = orderRepository.findByStatusIn(
                List.of(RacketRentalOrder.OrderStatus.PENDING, RacketRentalOrder.OrderStatus.PAID)
        );

        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (RacketRentalOrder rental : activeRentals) {
            if (rental.getBookingId() != null) {
                Booking booking = bookingRepository.findById(rental.getBookingId()).orElse(null);
                if (booking != null) {
                    LocalDateTime endDateTime = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
                    if (endDateTime.isBefore(now)) {
                        for (RacketRentalOrderDetail detail : rental.getDetails()) {
                            Racket racket = detail.getRacket();
                            racket.setStockQuantity(racket.getStockQuantity() + detail.getQuantity());
                            racketRepository.save(racket);
                        }
                        rental.setStatus(RacketRentalOrder.OrderStatus.RETURNED);
                        orderRepository.save(rental);
                        count++;
                    }
                }
            }
        }
        if (count > 0) {
            log.info("Auto-returned rackets for {} expired bookings.", count);
        }
    }
}
