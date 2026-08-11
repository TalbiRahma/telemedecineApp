package com.telemedecine.api.controller;

import com.telemedecine.api.dao.ConsultationRepository;
import com.telemedecine.api.dto.ConsultationDTO;
import com.telemedecine.api.mapper.ConsultationMapper;
import com.telemedecine.api.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationRepository consultationRepository;
    private final ConsultationMapper consultationMapper;

    //  Créer une nouvelle consultation
    @PostMapping
    public ResponseEntity<ConsultationDTO> createConsultation(@RequestBody ConsultationDTO dto) {
        ConsultationDTO created = consultationService.createConsultation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    //  Annuler une consultation
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelConsultation(@PathVariable Long id) {
        consultationService.cancelConsultation(id);
        return ResponseEntity.noContent().build();
    }

    //  Récupérer une consultation par ID
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationDTO> getConsultation(@PathVariable Long id) {
        ConsultationDTO dto = consultationService.getConsultation(id);
        return ResponseEntity.ok(dto);
    }

    //  Lister toutes les consultations
    @GetMapping
    public ResponseEntity<List<ConsultationDTO>> getAllConsultations() {
        var list = consultationRepository.findAll()
                .stream()
                .map(consultationMapper::toDto)
                .toList();

        return ResponseEntity.ok(list);
    }
}
