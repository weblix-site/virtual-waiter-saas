package md.virtualwaiter.service;

import md.virtualwaiter.domain.StaffDeviceToken;
import md.virtualwaiter.repo.StaffDeviceTokenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class StaffPushService {
  private static final Logger log = LoggerFactory.getLogger(StaffPushService.class);
  private final StaffDeviceTokenRepo tokenRepo;

  public StaffPushService(StaffDeviceTokenRepo tokenRepo) {
    this.tokenRepo = tokenRepo;
  }

  public void notifyBranch(long branchId, String type, long refId) {
    List<StaffDeviceToken> tokens = tokenRepo.findByBranchId(branchId);
    for (StaffDeviceToken t : tokens) {
      // Placeholder for real push provider (FCM/APNs)
      log.info("[PUSH] branch={} token={} platform={} type={} refId={}", branchId, t.token, t.platform, type, refId);
      t.lastSeenAt = Instant.now();
      tokenRepo.save(t);
    }
  }
}
