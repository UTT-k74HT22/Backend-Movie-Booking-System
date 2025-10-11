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

//NOTE sửa lại nhé

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

    //public
    public String generateToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", account.getId());
        claims.put("username", account.getUsername());
        claims.put("email", account.getEmail());
        claims.put("status", account.getStatus().toString());
        claims.put("emailVerified", account.getEmailVerified());
        return generateToken(claims, account);
    }

    private String generateToken(Map<String, Object> extraClaims, Account account) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(account.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (expiryMinutes * 60 * 1000)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    //fix lại
    public String generateRefreshToken(Account account) {
        return Jwts.builder()
                .setSubject(account.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (expiryDay * 24 * 60 * 60 * 1000)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // => xử lí build token: AccessToken và RefreshToken
    //Chú ý public và private

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractAccountId(String token) {
        return extractClaim(token, claims -> claims.get("accountId", Long.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractStatus(String token) {
        return extractClaim(token, claims -> claims.get("status", String.class));
    }

    public Boolean extractEmailVerified(String token) {
        return extractClaim(token, claims -> claims.get("emailVerified", Boolean.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, Account account) {
        final String username = extractUsername(token);
        return (username.equals(account.getUsername()) && !isTokenExpired(token));
    }

    //validate chuẩn lại
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(accessKey).parseClaimsJws(authToken);
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
