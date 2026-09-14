package com.lost.link.lost.link_backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.lost.link.lost.link_backend.service.UserService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:5174"
}, allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS })
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody(required = false) Map<String, String> payload) {
        Map<String, String> safePayload = payload != null ? payload : Map.of();
        String name = safePayload.getOrDefault("name", "");
        String email = safePayload.getOrDefault("email", "");
        String password = safePayload.getOrDefault("password", "");
        Map<String, Object> response = userService.register(name, email, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, String> payload) {
        Map<String, String> safePayload = payload != null ? payload : Map.of();
        String email = safePayload.getOrDefault("email", "");
        String password = safePayload.getOrDefault("password", "");
        Map<String, Object> response = userService.login(email, password);
        return ResponseEntity.ok(response);
    }
}
