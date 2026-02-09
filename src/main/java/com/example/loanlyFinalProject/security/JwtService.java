package com.example.loanlyFinalProject.security;

import com.example.loanlyFinalProject.service.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final TokenBlacklistService tokenBlacklistService;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private long jwtExpiration;

  public String generateToken(Authentication authentication) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    return generateToken(userDetails);
  }

  public String generateToken(CustomUserDetails userDetails) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .subject(userDetails.getId().toString())
        .claim("username", userDetails.getUsername())
        .claim("email", userDetails.getEmail())
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public Long getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return Long.parseLong(claims.getSubject());
  }

  public String getUsernameFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return claims.get("username", String.class);
  }

  public boolean validateToken(String token) {
    try {
      // Check if token is blacklisted first
      if (tokenBlacklistService.isBlacklisted(token)) {
        return false;
      }

      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (MalformedJwtException
        | ExpiredJwtException
        | UnsupportedJwtException
        | IllegalArgumentException e) {
      return false;
    }
  }

  public long getExpirationTime() {
    return jwtExpiration;
  }

  /** Get remaining time until token expires. */
  public long getRemainingExpirationTime(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

      Date expiration = claims.getExpiration();
      long remainingMs = expiration.getTime() - System.currentTimeMillis();
      return Math.max(remainingMs, 0);
    } catch (Exception e) {
      return 0;
    }
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
