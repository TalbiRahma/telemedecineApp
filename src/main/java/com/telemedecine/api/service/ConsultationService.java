package com.telemedecine.api.service;

import com.telemedecine.api.dto.ConsultationDTO;
import com.telemedecine.api.model.consultation.Consultation;

public interface ConsultationService {
     ConsultationDTO createConsultation(ConsultationDTO dto);
     void cancelConsultation(Long id) ;
    ConsultationDTO getConsultation(Long id);
}
