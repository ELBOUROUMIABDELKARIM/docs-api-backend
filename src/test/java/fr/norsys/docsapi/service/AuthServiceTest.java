package fr.norsys.docsapi.service;

import fr.norsys.docsapi.dto.auth.JwtResponse;
import fr.norsys.docsapi.dto.auth.LoginRequest;
import fr.norsys.docsapi.dto.auth.SignupRequest;
import fr.norsys.docsapi.entity.User;
import fr.norsys.docsapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Objects;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthServiceTest extends MyContainer {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully signup")
    public void testSignupSuccessful() {
        SignupRequest signupRequest = new SignupRequest("user1", "user1@example.com", "password123");

        ResponseEntity<String> response = authService.signup(signupRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("User registered successfully!");

        Optional<User> userOptional = userRepository.findByUserName(signupRequest.getUsername());
        assertThat(userOptional).isPresent();
        User user = userOptional.get();
        assertThat(passwordEncoder.matches(signupRequest.getPassword(), user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Should handle duplicate username during signup")
    public void testSignupDuplicateUsername() {
        SignupRequest signupRequest = new SignupRequest("user1", "user1@example.com", "password123");
        authService.signup(signupRequest);

        SignupRequest duplicateUserNameRequest = new SignupRequest("user1", "user2@example.com", "password123");

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->authService.signup(duplicateUserNameRequest));
        assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseStatusException.getBody().getDetail()).isEqualTo("Username is already taken!");
    }

    @Test
    @DisplayName("Should handle duplicate email during signup")
    public void testSignupDuplicateEmail() {
        SignupRequest signupRequest = new SignupRequest("user1", "user1@example.com", "password123");
        authService.signup(signupRequest);

        SignupRequest duplicateEmailRequest = new SignupRequest("user2", "user1@example.com", "password456");
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->authService.signup(duplicateEmailRequest));
        assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseStatusException.getBody().getDetail()).isEqualTo("Email is already in use!");

    }

    @Test
    @DisplayName("Should successfully login")
    public void testLoginSuccessful() {
        SignupRequest signupRequest = new SignupRequest("user1", "user1@example.com", "password123");
        authService.signup(signupRequest);

        LoginRequest loginRequest = new LoginRequest(signupRequest.getUsername(), signupRequest.getPassword());
        ResponseEntity<JwtResponse> response = authService.login(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody()).getToken()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle invalid login credentials")
    public void testLoginInvalidCredentials() {
        SignupRequest signupRequest = new SignupRequest("user1", "user1@example.com", "password123");
        authService.signup(signupRequest);

        LoginRequest invalidLoginRequest = new LoginRequest(signupRequest.getUsername(), "password");

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->authService.login(invalidLoginRequest));
        assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseStatusException.getBody().getDetail()).isEqualTo("Invalid Credentials");
    }
}
