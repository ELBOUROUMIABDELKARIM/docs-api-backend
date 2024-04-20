package fr.norsys.docsapi.service;

import fr.norsys.docsapi.dto.MessageResponse;
import fr.norsys.docsapi.dto.auth.JwtResponse;
import fr.norsys.docsapi.dto.auth.LoginRequest;
import fr.norsys.docsapi.dto.auth.SignupRequest;
import fr.norsys.docsapi.entity.User;
import fr.norsys.docsapi.repository.UserRepository;
import fr.norsys.docsapi.security.service.UserDetailsImpl;
import fr.norsys.docsapi.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Karim
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder encoder;


    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail()
        );
    }

    public MessageResponse signup(SignupRequest signupRequest) {

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return new MessageResponse(HttpStatus.BAD_REQUEST.value(), "Email is already in use!");
        }
        if (userRepository.existsByUserName(signupRequest.getUsername())) {
            return new MessageResponse(HttpStatus.BAD_REQUEST.value(), "Username is already taken!");
        }
        User user = User.builder()
                .userName(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(encoder.encode(signupRequest.getPassword()))
                .build();

        userRepository.save(user);
        return new MessageResponse(HttpStatus.CREATED.value(), "User registered successfully!");
    }


}
