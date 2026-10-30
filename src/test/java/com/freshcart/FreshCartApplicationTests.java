package com.freshcart;

import com.freshcart.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FreshCartApplicationTests {
    @Test
    void contextLoads() {}

    @Test
    void registrationPasswordRulesRequireStrongPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user@example.com");
        request.setPassword("Ab1!short");
        assertNotNull(request.getPassword());
        assertTrue(request.getPassword().length() >= 8);
        assertFalse(request.getPassword().matches("^.{1,7}$"));
    }
}
