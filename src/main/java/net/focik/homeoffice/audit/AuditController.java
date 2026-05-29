package net.focik.homeoffice.audit;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<AuditEntryDto>> getLatestEntries(
            @RequestParam String entityType,
            @RequestParam(defaultValue = "5") int limit) {

        List<AuditEntry> entries = auditService.getLatestEntries(entityType, limit);
        List<AuditEntryDto> dtos = entries.stream()
                .map(entry -> modelMapper.map(entry, AuditEntryDto.class))
                .toList();

        return ResponseEntity.ok(dtos);
    }
}
