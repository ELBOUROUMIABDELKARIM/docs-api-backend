package fr.norsys.docsapi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotNull(message = "Username cannot be null")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 10 characters")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be null")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 20 characters")
    private String password;

    public String toString(){
        return "username: " + username + " email: " + email + " password: " + password;
    }
}
