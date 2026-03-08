package org.nahap.commentratingservice.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple Principal class to hold JWT user information without DB lookup
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserPrincipal {
    private Integer userId;
    private String username;
    private String role; // Single role from JWT

    public boolean hasRole(String roleName) {
        return role != null && role.equals(roleName);
    }
}
