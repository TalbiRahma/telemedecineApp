package com.telemedecine.api;

import com.telemedecine.api.auth.AuthenticationService;
import com.telemedecine.api.auth.RegisterRequest;
import com.telemedecine.api.dto.SpecialtyDto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static com.telemedecine.api.model.user.Role.*;

@SpringBootApplication
public class TelemedecineApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelemedecineApplication.class, args);
	}

    @Bean
    public CommandLineRunner commandLineRunner(
            AuthenticationService service
    ) {
       return args -> {
            var admin = RegisterRequest.builder()
                    .firstname("admin")
                    .lastname("admin")
                    .email("admin@gmail.com")
                    .password("admin")
                    .role(ADMIN)
                    .build();
            System.out.println("Admin token: " + service.register(admin).getAccessToken());

            var doctor = RegisterRequest.builder()
                    .firstname("doctor")
                    .lastname("doctor")
                    .email("doctor@gmail.com")
                    .password("doctor")
                    .role(DOCTOR)
                    .build();
            System.out.println("Doctor token: " + service.register(doctor).getAccessToken());

            var patient = RegisterRequest.builder()
                    .firstname("patient")
                    .lastname("patient")
                    .email("patient@gmail.com")
                    .password("patient")
                    .role(PATIENT)
                    .build();
            System.out.println("Patient token: " + service.register(patient).getAccessToken());

            var specialty = SpecialtyDto.builder()
                    .name("CARDIOLOGY")
                    .description("specialty description")
                    .build();

        };

    }
}
