package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.BookingDTO;
import com.example.CauLongVui.entity.Booking;
import com.example.CauLongVui.entity.Court;
import com.example.CauLongVui.exception.BadRequestException;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.BookingRepository;
import com.example.CauLongVui.repository.CourtRepository;
import com.example.CauLongVui.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final BookingHoldService bookingHoldService;

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

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserIdOrderByBookingDateDesc(userId).stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getPassBookings() {
        return bookingRepository.findByIsPassTrueOrderByBookingDateDesc().stream()
                .map(BookingDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getBookedSlots(Long courtId, LocalDate date) {
        return bookingRepository.findByCourtIdAndBookingDate(courtId, date).stream()
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .map(b -> Map.of(
                        "startTime", b.getStartTime().toString(),
                        "endTime",   b.getEndTime().toString()
                ))
                .collect(Collectors.toList());
    }

    public BookingDTO createBooking(BookingDTO bookingDTO, String holdId) {
        // Validate court exists
        Court court = courtRepository.findById(bookingDTO.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân với ID: " + bookingDTO.getCourtId()));

        // Validate time
        if (!bookingDTO.getStartTime().isBefore(bookingDTO.getEndTime())) {
            throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc");
        }

        // If holdId is provided, verify it — otherwise fall back to DB overlap check
        if (holdId != null && !holdId.isBlank()) {
            boolean holdValid = bookingHoldService.confirmHold(holdId);
            if (!holdValid) {
                throw new BadRequestException("Giữ chỗ đã hết hạn hoặc không hợp lệ. Vui lòng giữ chỗ lại.");
            }
        } else {
            // Legacy path: check DB overlaps directly
            List<Booking> dayBookings = bookingRepository.findByCourtIdAndBookingDate(bookingDTO.getCourtId(), bookingDTO.getBookingDate());
            boolean hasOverlap = dayBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                    .anyMatch(b -> bookingDTO.getStartTime().isBefore(b.getEndTime()) && bookingDTO.getEndTime().isAfter(b.getStartTime()));

            if (hasOverlap) {
                throw new BadRequestException("Sân đã được đặt trong khung giờ này. Vui lòng chọn khung giờ khác.");
            }
        }

        // Calculate total price
        long hours = Duration.between(bookingDTO.getStartTime(), bookingDTO.getEndTime()).toMinutes();
        double totalPrice = (hours / 60.0) * court.getPricePerHour();

        Booking booking = Booking.builder()
                .court(court)
                .user(bookingDTO.getUserId() != null ?
                        userRepository.findById(bookingDTO.getUserId()).orElse(null) : null)
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

    /** Overload for backward compatibility (no holdId). */
    public BookingDTO createBooking(BookingDTO bookingDTO) {
        return createBooking(bookingDTO, null);
    }

    public BookingDTO updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân với ID: " + id));
        booking.setStatus(status);
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDTO updateBookingToPass(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân với ID: " + id));
        booking.setIsPass(true);
        booking.setPassPrice(booking.getTotalPrice() != null ? booking.getTotalPrice() * 0.8 : 0.0);
        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDTO buyPassBooking(Long id, Long newUserId, String newCustomerName, String newCustomerPhone) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân với ID: " + id));
        if (!Boolean.TRUE.equals(booking.getIsPass())) {
            throw new BadRequestException("Sân này không được đăng bán lại.");
        }

        booking.setIsPass(false);
        if (booking.getPassPrice() != null) {
            booking.setTotalPrice(booking.getPassPrice());
        }
        booking.setPassPrice(null);
        
        if (newUserId != null) {
            booking.setUser(userRepository.findById(newUserId).orElse(null));
        }
        booking.setCustomerName(newCustomerName);
        booking.setCustomerPhone(newCustomerPhone);

        return BookingDTO.fromEntity(bookingRepository.save(booking));
    }
}
