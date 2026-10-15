package com.rentamovil.payment.domain.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Quién hace la petición, según los claims del access token. */
@Getter
@AllArgsConstructor
public class AuthenticatedUser {

    /** Claim sub: iam.[user].user_id. Es quien queda en payment.reviewed_by al aprobar/rechazar. */
    private Integer userId;

    /** Claim personId: iam.person.person_id. */
    private Integer personId;

    private String role;

    private List<String> permissions;

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}
