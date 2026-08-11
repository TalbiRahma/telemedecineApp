package com.telemedecine.api.controller;

import com.telemedecine.api.dao.ConsultationRepository;
import com.telemedecine.api.model.consultation.ConsultationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/zoom/webhook")
@RequiredArgsConstructor
public class ZoomWebhookController {

    private final ConsultationRepository consultationRepository;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        String event = (String) payload.get("event");

        Map<String, Object> meetingData = (Map<String, Object>) ((Map<String, Object>) payload.get("payload")).get("object");
        String meetingId = String.valueOf(meetingData.get("id"));

        consultationRepository.findByZoomMeetingId(meetingId).ifPresent(consultation -> {
            switch (event) {
                case "meeting.started" -> consultation.setStatus(ConsultationStatus.ONGOING);
                case "meeting.ended" -> consultation.setStatus(ConsultationStatus.COMPLETED);
                case "meeting.deleted" -> consultation.setStatus(ConsultationStatus.CANCELLED);
            }
            consultationRepository.save(consultation);
        });

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
