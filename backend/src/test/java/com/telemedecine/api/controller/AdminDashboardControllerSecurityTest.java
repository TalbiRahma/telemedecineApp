package com.telemedecine.api.controller;

import com.telemedecine.api.dto.AdminOverviewStatsDto;
import com.telemedecine.api.service.AdminDashboardService;
import com.telemedecine.api.security.JwtAuthenticationFilter;
import com.telemedecine.api.auth.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminDashboardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(AdminDashboardControllerSecurityTest.TestSecurityConfiguration.class)
class AdminDashboardControllerSecurityTest {
    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminReceivesOverview() throws Exception {
        when(adminDashboardService.getOverviewStats()).thenReturn(
                new AdminOverviewStatsDto(9, 5, 2, 2, 1, 3, 12));

        mockMvc.perform(get("/api/v1/admin/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(9))
                .andExpect(jsonPath("$.pendingDoctors").value(2))
                .andExpect(jsonPath("$.totalAppointments").value(12));
    }

    @Test
    @WithMockUser(username = "patient@example.com", roles = "PATIENT")
    void nonAdminReceivesForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfiguration {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(basic -> {})
                    .build();
        }
    }
}
