package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.RacketDTO;
import com.example.CauLongVui.service.RacketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rackets")
@RequiredArgsConstructor
public class RacketController {

    private final RacketService racketService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RacketDTO>>> getAllRackets(
            @RequestParam(name = "search", required = false) String search) {
        List<RacketDTO> rackets = (search != null && !search.isBlank())
                ? racketService.searchByName(search)
                : racketService.getAllRackets();
        return ResponseEntity.ok(ApiResponse.success(rackets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RacketDTO>> getRacketById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(racketService.getRacketById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RacketDTO>> createRacket(@RequestBody RacketDTO dto) {
        RacketDTO created = racketService.createRacket(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm vợt thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RacketDTO>> updateRacket(@PathVariable Long id, @RequestBody RacketDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vợt thành công", racketService.updateRacket(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRacket(@PathVariable Long id) {
        racketService.deleteRacket(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa vợt thành công", null));
    }
}
