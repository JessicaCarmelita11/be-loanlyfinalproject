package com.example.loanlyFinalProject.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for managing JWT token blacklist using Redis. Tokens are stored with TTL matching their
 * expiration time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String BLACKLIST_PREFIX = "blacklist:";

  /**
   * Add a token to the blacklist.
   *
   * @param token The JWT token to blacklist
   * @param expirationTimeMs Time until token expires (in milliseconds)
   */
  public void blacklistToken(String token, long expirationTimeMs) {
    String key = BLACKLIST_PREFIX + token;
    // Store with TTL so it auto-expires when the token would have expired anyway
    redisTemplate.opsForValue().set(key, "revoked", expirationTimeMs, TimeUnit.MILLISECONDS);
    log.info("Token blacklisted, will expire in {} ms", expirationTimeMs);
  }

  /**
   * Check if a token is blacklisted.
   *
   * @param token The JWT token to check
   * @return true if the token is blacklisted, false otherwise
   */
  public boolean isBlacklisted(String token) {
    String key = BLACKLIST_PREFIX + token;
    Boolean exists = redisTemplate.hasKey(key);
    return exists != null && exists;
  }
}
