package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.Racket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RacketRepository extends JpaRepository<Racket, Long> {
    List<Racket> findByNameContainingIgnoreCase(String name);
}
