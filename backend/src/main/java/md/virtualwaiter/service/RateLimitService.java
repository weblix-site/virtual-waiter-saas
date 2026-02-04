package md.virtualwaiter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PreDestroy;
import org.springframework.lang.Nullable;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Service
public class RateLimitService {
  private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
  private final String redisHost;
  private final int redisPort;
  private final String redisPassword;
  private final boolean redisEnabled;
  private final JedisPool jedisPool;
  private final int redisTimeoutMs;
  private final AtomicLong redisFailureCount = new AtomicLong(0);
  private final Counter redisFailureCounter;

  public RateLimitService(
    @Value("${app.rateLimit.redis.host:}") String redisHost,
    @Value("${app.rateLimit.redis.port:6379}") int redisPort,
    @Value("${app.rateLimit.redis.password:}") String redisPassword,
    @Value("${app.rateLimit.redis.enabled:false}") boolean redisEnabled,
    @Value("${app.rateLimit.redis.timeoutMs:2000}") int redisTimeoutMs,
    @Nullable MeterRegistry meterRegistry
  ) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
    this.redisPassword = redisPassword;
    this.redisEnabled = redisEnabled;
    this.redisTimeoutMs = redisTimeoutMs;
    this.jedisPool = buildPool();
    this.redisFailureCounter = meterRegistry == null
      ? null
      : meterRegistry.counter("vw.ratelimit.redis.failures");
  }

  public boolean allow(String key, int limit, int windowSeconds) {
    if (limit <= 0 || windowSeconds <= 0) return true;
    if (jedisPool != null) {
      try (Jedis jedis = jedisPool.getResource()) {
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
        redisFailureCount.incrementAndGet();
        if (redisFailureCounter != null) {
          redisFailureCounter.increment();
        }
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

  private JedisPool buildPool() {
    if (!redisEnabled || redisHost == null || redisHost.isBlank()) return null;
    JedisPoolConfig config = new JedisPoolConfig();
    config.setMaxTotal(16);
    config.setMaxIdle(8);
    config.setMinIdle(1);
    config.setMaxWaitMillis(redisTimeoutMs);
    config.setBlockWhenExhausted(true);
    config.setTestOnBorrow(true);
    config.setTestWhileIdle(true);
    if (redisPassword != null && !redisPassword.isBlank()) {
      return new JedisPool(config, redisHost, redisPort, redisTimeoutMs, redisPassword);
    }
    return new JedisPool(config, redisHost, redisPort, redisTimeoutMs);
  }

  @PreDestroy
  void closePool() {
    if (jedisPool != null) {
      jedisPool.close();
    }
  }
}
