package com.legacyvault.service;

import com.legacyvault.dto.Actor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedActorProvider {

    public Actor requireActor(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            throw Flow5Exception.unauthenticated();
        }

        String userId = request.getUserPrincipal().getName();
        try {
            UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw Flow5Exception.unauthenticated();
        }

        return new Actor(
                userId,
                request.isUserInRole("LEGAL_VERIFIER")
        );
    }
}
