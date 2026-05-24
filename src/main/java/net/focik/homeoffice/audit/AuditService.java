package net.focik.homeoffice.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;

    public void log(String entityType, String entityId, AuditAction action, String newValues) {
        log(entityType, entityId, action, newValues, null);
    }

    public void log(String entityType, String entityId, AuditAction action, String newValues, String changedBy) {
        try {
            AuditEntry entry = AuditEntry.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .changedBy(changedBy)
                    .newValues(newValues)
                    .build();

            auditEntryRepository.save(entry);
        } catch (Exception e) {
            // Audit failure should not break business logic
        }
    }
}
