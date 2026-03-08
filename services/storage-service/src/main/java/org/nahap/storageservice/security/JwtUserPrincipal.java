package org.nahap.storageservice.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserPrincipal {
    private Integer userId;
    private String username;
    private String role;

    public boolean hasRole(String roleName) {
        return role != null && role.equals(roleName);
    }
}
