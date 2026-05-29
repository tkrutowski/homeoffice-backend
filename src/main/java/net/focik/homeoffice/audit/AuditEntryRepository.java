package net.focik.homeoffice.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    @Query(value = "SELECT * FROM audit_log WHERE entity_type = :entityType ORDER BY changed_at DESC LIMIT :limit",
           nativeQuery = true)
    List<AuditEntry> findLatestByEntityType(@Param("entityType") String entityType, @Param("limit") int limit);
}
