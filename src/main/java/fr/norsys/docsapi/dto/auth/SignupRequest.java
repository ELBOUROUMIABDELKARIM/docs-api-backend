package fr.norsys.docsapi.dto.auth;

import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String email;
    private String password;

    public String toString(){
        return "username: " + username + " email: " + email + " password: " + password;
    }
}
