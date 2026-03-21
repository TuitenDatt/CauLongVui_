package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCourtId(Long courtId);

    List<Booking> findByCustomerPhoneOrderByBookingDateDesc(String phone);


    List<Booking> findByBookingDate(LocalDate date);

    List<Booking> findByCourtIdAndBookingDate(Long courtId, LocalDate date);
}
