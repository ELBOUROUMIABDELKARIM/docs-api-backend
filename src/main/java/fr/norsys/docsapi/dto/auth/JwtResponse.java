package fr.norsys.docsapi.dto.auth;

import lombok.Data;

import java.util.UUID;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private UUID id;

    public JwtResponse(String token, UUID id) {
        this.token = token;
        this.id = id;
    }
}
