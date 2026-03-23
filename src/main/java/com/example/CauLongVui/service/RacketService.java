package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.RacketDTO;
import com.example.CauLongVui.entity.Racket;
import com.example.CauLongVui.repository.RacketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RacketService {

    private final RacketRepository racketRepository;

    public List<RacketDTO> getAllRackets() {
        return racketRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<RacketDTO> searchByName(String name) {
        return racketRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public RacketDTO getRacketById(Long id) {
        Racket racket = racketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Racket not found"));
        return mapToDTO(racket);
    }

    public RacketDTO createRacket(RacketDTO dto) {
        Racket racket = mapToEntity(dto);
        return mapToDTO(racketRepository.save(racket));
    }

    public RacketDTO updateRacket(Long id, RacketDTO dto) {
        Racket existing = racketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Racket not found"));

        if(dto.getName() != null) existing.setName(dto.getName());
        if(dto.getRacketType() != null) existing.setRacketType(dto.getRacketType());
        if(dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if(dto.getRentalPrice() != null) existing.setRentalPrice(dto.getRentalPrice());
        if(dto.getStockQuantity() != null) existing.setStockQuantity(dto.getStockQuantity());
        if(dto.getImageUrl() != null) existing.setImageUrl(dto.getImageUrl());

        return mapToDTO(racketRepository.save(existing));
    }

    public void deleteRacket(Long id) {
        if (!racketRepository.existsById(id)) {
            throw new RuntimeException("Racket not found");
        }
        racketRepository.deleteById(id);
    }

    private RacketDTO mapToDTO(Racket racket) {
        return RacketDTO.builder()
                .id(racket.getId())
                .name(racket.getName())
                .racketType(racket.getRacketType())
                .description(racket.getDescription())
                .rentalPrice(racket.getRentalPrice())
                .stockQuantity(racket.getStockQuantity())
                .imageUrl(racket.getImageUrl())
                .build();
    }

    private Racket mapToEntity(RacketDTO dto) {
        Racket racket = new Racket();
        racket.setName(dto.getName());
        racket.setRacketType(dto.getRacketType());
        racket.setDescription(dto.getDescription());
        racket.setRentalPrice(dto.getRentalPrice());
        racket.setStockQuantity(dto.getStockQuantity());
        racket.setImageUrl(dto.getImageUrl());
        return racket;
    }
}
