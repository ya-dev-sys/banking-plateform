package com.yanis.auth_service.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String passwordHash;

    @Builder.Default
    private Set<String> roles = new HashSet<>();

    private LocalDateTime createdAt;

    public void addRole(String role) {
        this.roles.add(role);
    }
}
