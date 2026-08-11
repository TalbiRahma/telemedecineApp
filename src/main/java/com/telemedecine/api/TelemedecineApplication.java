package com.telemedecine.api;

import com.telemedecine.api.auth.AuthenticationService;
import com.telemedecine.api.auth.RegisterRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelemedecineApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelemedecineApplication.class, args);
	}

 @Bean
    public CommandLineRunner commandLineRunner(
            AuthenticationService service,
            @Value("${app.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password
    ) {
       return args -> {
           if (!enabled) {
               return;
           }
           if (email.isBlank() || password.length() < 8) {
               throw new IllegalStateException(
                       "Bootstrap admin is enabled but its email/password configuration is invalid.");
           }
            var admin = RegisterRequest.builder()
                    .firstname("admin")
                    .lastname("admin")
                    .email(email)
                    .password(password)
                    .build();
            service.createBootstrapAdmin(admin);

        };

    }
}
