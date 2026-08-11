package com.telemedecine.api.controller;

import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.service.DoctorAvailabilityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorAvailabilityControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meOccurrencesUsesExplicitRouteAndAuthenticatedDoctorId() throws Exception {
        DoctorAvailabilityService service = mock(DoctorAvailabilityService.class);
        DoctorAvailabilityController controller = new DoctorAvailabilityController(service);
        Doctor doctor = new Doctor();
        doctor.setId(17L);
        doctor.setEmail("doctor@example.com");
        doctor.setRole(Role.DOCTOR);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        doctor, null, doctor.getAuthorities()));

        LocalDate from = LocalDate.of(2026, 8, 3);
        LocalDate to = LocalDate.of(2026, 8, 9);
        when(service.getDoctorAvailabilityOccurrences(17L, from, to, false))
                .thenReturn(List.of());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        mockMvc.perform(get("/api/availabilities/me/occurrences")
                        .param("from", "2026-08-03")
                        .param("to", "2026-08-09")
                        .param("includeSlots", "false"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(service).getDoctorAvailabilityOccurrences(17L, from, to, false);
    }
}
