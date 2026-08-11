package com.telemedecine.api.config;

import com.telemedecine.api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
      http
              .cors()
              .and()
              .csrf().disable()
              .authorizeHttpRequests()
              .requestMatchers(HttpMethod.GET, "/api/v1/health")
              .permitAll()
              .requestMatchers(HttpMethod.GET,
                      "/api/v1/specialties/all",
                      "/api/v1/specialties/{id}")
              .permitAll()
              .requestMatchers(
                      "/api/v1/auth/register",
                      "/api/v1/auth/register/doctor",
                      "/api/v1/auth/authenticate",
                      "/api/v1/auth/mfa/verify",
                      "/api/v1/auth/mfa/enroll/verify",
                      "/api/v1/auth/mfa/enroll/resume",
                      "/api/v1/auth/refresh-token",
                      "/api/v1/auth/forgot-password",
                      "/api/v1/auth/reset-password",
                      "/v2/api-docs",
                      "/v3/api-docs",
                      "/v3/api-docs/**",
                      "/swagger-resources",
                      "/swagger-resources/**",
                      "/configuration/ui",
                      "/configuration/security",
                      "/swagger-ui/**",
                      "/webjars/**",
                      "/swagger-ui.html")
              .permitAll()
              .anyRequest()
              .authenticated()
              .and()
              .sessionManagement()
              .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
              .and()
              .authenticationProvider(authenticationProvider)
              .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .exceptionHandling(exceptions -> exceptions
                      .authenticationEntryPoint((request, response, exception) ->
                              response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")))
              .logout(logout -> logout
                      .logoutUrl("/api/v1/auth/logout")
                      .addLogoutHandler(logoutHandler)
                      .logoutSuccessHandler((request, response, authentication) ->
                              SecurityContextHolder.clearContext()
                      )
              );

        return http.build();
    }
}
