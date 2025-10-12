package com.trainning.movie_booking_system.security;

import com.trainning.movie_booking_system.entity.Account;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtProvider {

    @Value("${jwt.accessKey}")
    private String accessKey;

    @Value("${jwt.expiryMinutes}")
    private int expiryMinutes;

    @Value("${jwt.expiryDay}")
    private int expiryDay;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(accessKey.getBytes());
    }

    public String generateToken(Account account) {
        String roles = account.getAccountRoles().stream()
                .map(role -> "ROLE_" + role.getRole().getName())
                .collect(Collectors.joining(","));

        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", account.getId());
        claims.put("roles", roles);
        return generateToken(claims, account);
    }

    private String generateToken(Map<String, Object> extraClaims, Account account) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(account.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ((long) expiryMinutes * 60 * 1000)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Account account) {
        return Jwts.builder()
                .setSubject(account.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ((long) expiryDay * 24 * 60 * 60 * 1000)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }

    //========== PRIVATE METHOD ==========//
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
