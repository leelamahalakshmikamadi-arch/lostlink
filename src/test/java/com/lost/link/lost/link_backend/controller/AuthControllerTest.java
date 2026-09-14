package com.lost.link.lost.link_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.lost.link.lost.link_backend.model.User;
import com.lost.link.lost.link_backend.repository.UserRepository;
import com.lost.link.lost.link_backend.service.UserService;

class AuthControllerTest {

    private UserRepository userRepository;
    private UserService userService;
    private AuthController authController;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
        authController = new AuthController(userService);
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void registerUniqueUserSucceeds() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("mock-id-123");
            return u;
        });

        ResponseEntity<Map<String, Object>> response = authController.register(
                Map.of("name", "Alice", "email", "alice@example.com", "password", "secret123"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration successful.", response.getBody().get("message"));
        assertNotNull(response.getBody().get("token"));
        assertTrue(response.getBody().containsKey("user"));
    }

    @Test
    void registerDuplicateEmailThrowsConflict() {
        User existing = new User();
        existing.setId("user-1");
        existing.setEmail("alice@example.com");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.register(Map.of("name", "Alice", "email", "alice@example.com", "password", "secret123")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("An account with this email already exists.", ex.getReason());

        // Verify GlobalExceptionHandler formats 409 response with clear message
        ResponseEntity<Map<String, Object>> handled = exceptionHandler.handleResponseStatusException(ex);
        assertEquals(HttpStatus.CONFLICT, handled.getStatusCode());
        assertEquals("An account with this email already exists.", handled.getBody().get("message"));
    }

    @Test
    void registerDuplicateKeyExceptionHandledAsConflict() {
        when(userRepository.findByEmailIgnoreCase("race@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.register(Map.of("name", "Race", "email", "race@example.com", "password", "secret123")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("An account with this email already exists.", ex.getReason());
    }

    @Test
    void registerMissingFieldsThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.register(Map.of("name", "", "email", "test@example.com", "password", "pass")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Name is required.", ex.getReason());
    }

    @Test
    void loginSuccessAndFailure() {
        User user = new User();
        user.setId("user-1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret123");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        // Successful login
        ResponseEntity<Map<String, Object>> successRes = authController.login(
                Map.of("email", "alice@example.com", "password", "secret123"));
        assertEquals(HttpStatus.OK, successRes.getStatusCode());
        assertEquals("Login successful.", successRes.getBody().get("message"));

        // Wrong password
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(Map.of("email", "alice@example.com", "password", "wrongpass")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid email or password.", ex.getReason());
    }
}

