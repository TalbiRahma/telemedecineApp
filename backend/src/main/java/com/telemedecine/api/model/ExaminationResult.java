package com.telemedecine.api.model;

import com.telemedecine.api.model.consultation.Consultation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "results")
public class ExaminationResult {
    @Id
    @GeneratedValue
    private Long id;
    private String type;
    private String description;
    private String fileUrl;
    private LocalDate date;

    @ManyToOne
    private Consultation consultation;
}
