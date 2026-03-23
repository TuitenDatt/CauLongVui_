package com.example.CauLongVui.repository;

import com.example.CauLongVui.entity.Matchmaking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MatchmakingRepository extends JpaRepository<Matchmaking, Long> {
    List<Matchmaking> findByStatusInAndPlayDateGreaterThanEqualOrderByPlayDateAsc(List<String> statuses, LocalDate date);
    List<Matchmaking> findByUserIdOrderByCreatedAtDesc(Long userId);
}
