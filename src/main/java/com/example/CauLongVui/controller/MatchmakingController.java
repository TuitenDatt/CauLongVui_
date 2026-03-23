package com.example.CauLongVui.controller;

import com.example.CauLongVui.entity.Matchmaking;
import com.example.CauLongVui.repository.MatchmakingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    @Autowired
    private MatchmakingRepository repo;

    private ResponseEntity<?> createResponse(boolean success, String message, Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("message", message);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    // Lấy danh sách ghép kèo đang "OPEN" tính từ hôm nay
    @GetMapping
    public ResponseEntity<?> getAllOpen() {
        try {
            LocalDate today = LocalDate.now();
            List<Matchmaking> list = repo.findByStatusInAndPlayDateGreaterThanEqualOrderByPlayDateAsc(Arrays.asList("OPEN", "FULL"), today);
            return createResponse(true, "Lấy danh sách thành công", list);
        } catch (Exception e) {
            return createResponse(false, e.getMessage(), null);
        }
    }

    // Lấy danh sách ghép kèo của 1 cá nhân
    @GetMapping("/my")
    public ResponseEntity<?> getMyMatchmakings(@RequestParam("userId") Long userId) {
        try {
            List<Matchmaking> list = repo.findByUserIdOrderByCreatedAtDesc(userId);
            return createResponse(true, "Thành công", list);
        } catch (Exception e) {
            return createResponse(false, e.getMessage(), null);
        }
    }

    // Tạo mới
    @PostMapping
    public ResponseEntity<?> createMatchmaking(@RequestBody Matchmaking m) {
        try {
            Matchmaking saved = repo.save(m);
            return createResponse(true, "Đăng ghép kèo thành công", saved);
        } catch (Exception e) {
            return createResponse(false, e.getMessage(), null);
        }
    }

    // Hủy hoặc Xóa kèo
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelMatchmaking(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        try {
            Optional<Matchmaking> opt = repo.findById(id);
            if(opt.isPresent()) {
                Matchmaking m = opt.get();
                if(!m.getUserId().equals(userId)) {
                    return createResponse(false, "Không có quyền hủy kèo này", null);
                }
                m.setStatus("CANCELLED");
                repo.save(m);
                return createResponse(true, "Hủy kèo thành công", null);
            }
            return createResponse(false, "Không tìm thấy kèo", null);
        } catch (Exception e) {
            return createResponse(false, e.getMessage(), null);
        }
    }

    // Tham gia kèo
    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinMatchmaking(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        try {
            Optional<Matchmaking> opt = repo.findById(id);
            if(opt.isPresent()) {
                Matchmaking m = opt.get();
                if(!"OPEN".equals(m.getStatus())) {
                    return createResponse(false, "Kèo này đã đủ người hoặc tự động đóng", null);
                }
                if(m.getUserId().equals(userId)) {
                    return createResponse(false, "Bạn không thực hiện được hành động này", null);
                }
                m.setJoinedPlayers((m.getJoinedPlayers() == null ? 0 : m.getJoinedPlayers()) + 1);
                if(m.getJoinedPlayers() >= m.getRequiredPlayers()) {
                    m.setStatus("FULL");
                }
                Matchmaking saved = repo.save(m);
                return createResponse(true, "Tham gia kèo thành công!", saved);
            }
            return createResponse(false, "Không tìm thấy kèo", null);
        } catch (Exception e) {
            return createResponse(false, e.getMessage(), null);
        }
    }
}
