package com.telemedecine.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.mail.username=test@example.com",
		"spring.mail.password=test-app-password"
})
class TelemedecineApplicationTests {

	@Test
	void contextLoads() {
	}

}
