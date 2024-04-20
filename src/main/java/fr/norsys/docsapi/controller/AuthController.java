package fr.norsys.docsapi.controller;

import fr.norsys.docsapi.dto.auth.LoginRequest;
import fr.norsys.docsapi.dto.MessageResponse;
import fr.norsys.docsapi.dto.auth.SignupRequest;
import fr.norsys.docsapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Karim
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        MessageResponse response = authService.signup(signUpRequest);
        if (response.getStatusCode() >= 400 && response.getStatusCode() < 500) {
            return ResponseEntity.badRequest().body(response);
        } else if (response.getStatusCode() >= 500) {
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response);
        }
    }
}
