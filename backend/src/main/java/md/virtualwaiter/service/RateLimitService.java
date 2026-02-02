package md.virtualwaiter.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
  private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

  public boolean allow(String key, int limit, int windowSeconds) {
    if (limit <= 0 || windowSeconds <= 0) return true;
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
