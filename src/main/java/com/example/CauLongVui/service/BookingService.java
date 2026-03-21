package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.BookingDTO;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.entity.Court;
import com.example.CauLongVui.exception.BadRequestException;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.BookingRepository;
import com.example.CauLongVui.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân với ID: " + id));
        return BookingDTO.fromEntity(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByCourtId(Long courtId) {
        return bookingRepository.findByCourtId(courtId).stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByPhone(String phone) {
        return bookingRepository.findByCustomerPhoneOrderByBookingDateDesc(phone).stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public BookingDTO createBooking(BookingDTO bookingDTO) {
        // Validate court exists
        Court court = courtRepository.findById(bookingDTO.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân với ID: " + bookingDTO.getCourtId()));

        // Validate time
        if (!bookingDTO.getStartTime().isBefore(bookingDTO.getEndTime())) {
            throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc");
        }

        // Check for overlapping bookings in Java to avoid SQL Server type conversion issues with LocalTime
        List<Booking> dayBookings = bookingRepository.findByCourtIdAndBookingDate(bookingDTO.getCourtId(), bookingDTO.getBookingDate());
        boolean hasOverlap = dayBookings.stream()
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .anyMatch(b -> bookingDTO.getStartTime().isBefore(b.getEndTime()) && bookingDTO.getEndTime().isAfter(b.getStartTime()));

        if (hasOverlap) {
            throw new BadRequestException("Sân đã được đặt trong khung giờ này. Vui lòng chọn khung giờ khác.");
        }

        // Calculate total price
        long hours = Duration.between(bookingDTO.getStartTime(), bookingDTO.getEndTime()).toMinutes();
        double totalPrice = (hours / 60.0) * court.getPricePerHour();

        Booking booking = Booking.builder()
                .court(court)
                .customerName(bookingDTO.getCustomerName())
                .customerPhone(bookingDTO.getCustomerPhone())
                .bookingDate(bookingDTO.getBookingDate())
                .startTime(bookingDTO.getStartTime())
                .endTime(bookingDTO.getEndTime())
                .totalPrice(totalPrice)
                .status(Booking.BookingStatus.PENDING)
                .build();

        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    public BookingDTO updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân với ID: " + id));
        booking.setStatus(status);
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }
}
