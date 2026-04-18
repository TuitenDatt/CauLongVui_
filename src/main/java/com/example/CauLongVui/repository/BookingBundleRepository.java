package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.BookingBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingBundleRepository extends JpaRepository<BookingBundle, Long> {
    List<BookingBundle> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
