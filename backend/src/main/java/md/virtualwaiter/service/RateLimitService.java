package md.virtualwaiter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import redis.clients.jedis.Jedis;

@Service
public class RateLimitService {
  private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
  private final String redisHost;
  private final int redisPort;
  private final String redisPassword;
  private final boolean redisEnabled;

  public RateLimitService(
    @Value("${app.rateLimit.redis.host:}") String redisHost,
    @Value("${app.rateLimit.redis.port:6379}") int redisPort,
    @Value("${app.rateLimit.redis.password:}") String redisPassword,
    @Value("${app.rateLimit.redis.enabled:false}") boolean redisEnabled
  ) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
    this.redisPassword = redisPassword;
    this.redisEnabled = redisEnabled;
  }

  public boolean allow(String key, int limit, int windowSeconds) {
    if (limit <= 0 || windowSeconds <= 0) return true;
    if (redisEnabled && redisHost != null && !redisHost.isBlank()) {
      try (Jedis jedis = new Jedis(redisHost, redisPort)) {
        if (redisPassword != null && !redisPassword.isBlank()) {
          jedis.auth(redisPassword);
        }
        String rKey = "rate:" + key;
        long now = Instant.now().getEpochSecond();
        long cutoff = now - windowSeconds;
        jedis.zremrangeByScore(rKey, 0, cutoff);
        long count = jedis.zcard(rKey);
        if (count >= limit) return false;
        jedis.zadd(rKey, now, String.valueOf(now) + ":" + Math.random());
        jedis.expire(rKey, windowSeconds + 5);
        return true;
      } catch (Exception e) {
        // fallback to in-memory
      }
    }
    return allowInMemory(key, limit, windowSeconds);
  }

  private boolean allowInMemory(String key, int limit, int windowSeconds) {
    long now = Instant.now().getEpochSecond();
    Deque<Long> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (q) {
      long cutoff = now - windowSeconds;
      while (!q.isEmpty() && q.peekFirst() <= cutoff) {
        q.pollFirst();
      }
      if (q.size() >= limit) {
        return false;
      }
      q.addLast(now);
      return true;
    }
  }
}
