package com.trainning.movie_booking_system.helpers;

import com.trainning.movie_booking_system.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtHelper {

    private final JwtService jwtService;

    /**
     * Extract account ID from JWT token
     */
    public Long getAccountIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtService.extractAccountId(token);
    }

    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtService.extractUsername(token);
    }

    /**
     * Extract email from JWT token
     */
    public String getEmailFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtService.extractEmail(token);
    }

    /**
     * Extract status from JWT token
     */
    public String getStatusFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtService.extractStatus(token);
    }

    /**
     * Check if token is valid
     */
    public boolean isTokenValid(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtService.validateToken(token);
    }

    /**
     * Check if user is admin (example)
     */
    public boolean isAdmin(String token) {
        if (!isTokenValid(token)) {
            return false;
        }
        String status = getStatusFromToken(token);
        return "ADMIN".equals(status);
    }

    /**
     * Check if user is active
     */
    public boolean isActive(String token) {
        if (!isTokenValid(token)) {
            return false;
        }
        String status = getStatusFromToken(token);
        return "ACTIVE".equals(status);
    }
}
