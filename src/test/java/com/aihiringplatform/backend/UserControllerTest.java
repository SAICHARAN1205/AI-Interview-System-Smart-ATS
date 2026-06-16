package com.aihiringplatform.backend;

import com.aihiringplatform.backend.controller.UserController;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.service.UserService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.aihiringplatform.backend.service.CaptchaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private CaptchaService captchaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterUser_Success() throws Exception {
        // Given
        ObjectNode registrationRequest = objectMapper.createObjectNode();
        registrationRequest.put("name", "John Doe");
        registrationRequest.put("email", "john@example.com");
        registrationRequest.put("password", "Password123!");
        registrationRequest.put("role", Role.CANDIDATE.name());

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setRole(Role.CANDIDATE);

        when(userService.registerUser(any(User.class))).thenReturn(java.util.Map.of(
            "email", "john@example.com",
            "role", Role.CANDIDATE,
            "verificationSessionId", "test-session-id"
        ));
        doNothing().when(captchaService).validateCaptcha(any(), any());

        // When & Then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.data.verificationSessionId").value("test-session-id"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    public void testRegisterUser_InvalidInput() throws Exception {
        // Given
        ObjectNode registrationRequest = objectMapper.createObjectNode();
        registrationRequest.put("name", ""); // Invalid: blank name
        registrationRequest.put("email", "invalid-email"); // Invalid: not email format
        registrationRequest.put("password", "");
        registrationRequest.putNull("role");

        // When & Then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegisterUser_InvalidCaptcha() throws Exception {
        // Given
        ObjectNode registrationRequest = objectMapper.createObjectNode();
        registrationRequest.put("name", "John Doe");
        registrationRequest.put("email", "john@example.com");
        registrationRequest.put("password", "Password123!");
        registrationRequest.put("role", Role.CANDIDATE.name());
        registrationRequest.put("captchaToken", "invalid");
        registrationRequest.put("captchaAnswer", "wrong");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect CAPTCHA. Please try again."))
                .when(captchaService).validateCaptcha(any(), any());

        // When & Then
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect CAPTCHA. Please try again."));
    }
}
