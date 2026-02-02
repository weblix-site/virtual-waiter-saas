package md.virtualwaiter.repo;

import md.virtualwaiter.domain.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findTop200ByBranchIdOrderByIdDesc(Long branchId);
  List<AuditLog> findTop200ByBranchIdAndIdLessThanOrderByIdDesc(Long branchId, Long beforeId);
  List<AuditLog> findTop200ByBranchIdAndIdGreaterThanOrderByIdDesc(Long branchId, Long afterId);

  @Query("""
    select a from AuditLog a
    where a.branchId = :branchId
      and (:action is null or a.action = :action)
      and (:entityType is null or a.entityType = :entityType)
      and (:actorUsername is null or a.actorUsername = :actorUsername)
      and (:fromTs is null or a.createdAt >= :fromTs)
      and (:toTs is null or a.createdAt <= :toTs)
      and (:beforeId is null or a.id < :beforeId)
      and (:afterId is null or a.id > :afterId)
    order by a.id desc
  """)
  List<AuditLog> findFiltered(
    @Param("branchId") Long branchId,
    @Param("action") String action,
    @Param("entityType") String entityType,
    @Param("actorUsername") String actorUsername,
    @Param("fromTs") java.time.Instant fromTs,
    @Param("toTs") java.time.Instant toTs,
    @Param("beforeId") Long beforeId,
    @Param("afterId") Long afterId,
    Pageable pageable
  );
}
