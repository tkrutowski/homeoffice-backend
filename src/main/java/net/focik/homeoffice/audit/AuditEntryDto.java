package net.focik.homeoffice.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryDto {

    private Long id;
    private String entityType;
    private String entityId;
    private AuditAction action;
    private String changedBy;
    private LocalDateTime changedAt;
    private String newValues;
    private String oldValues;
}
