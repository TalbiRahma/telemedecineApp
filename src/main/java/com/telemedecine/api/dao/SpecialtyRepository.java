package com.telemedecine.api.dao;

import com.telemedecine.api.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

}
