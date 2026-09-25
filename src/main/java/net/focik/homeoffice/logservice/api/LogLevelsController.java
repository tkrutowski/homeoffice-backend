package net.focik.homeoffice.logservice.api;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.logservice.api.dto.SetLogLevelRequest;
import net.focik.homeoffice.logservice.domain.model.LogLevelOverride;
import net.focik.homeoffice.logservice.domain.model.LogLevelsInfo;
import net.focik.homeoffice.logservice.domain.port.primary.ManageLogLevelsUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Tymczasowa zmiana poziomow logow (np. na DEBUG) na instancji, ktora obsluzy zadanie.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/logs/levels")
public class LogLevelsController {
    private final ManageLogLevelsUseCase manageLogLevelsUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<LogLevelsInfo> getLevels() {
        return new ResponseEntity<>(manageLogLevelsUseCase.getLevels(), HttpStatus.OK);
    }

    @PutMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<LogLevelOverride> setLevel(@RequestBody SetLogLevelRequest request, Principal principal) {
        LogLevelOverride override = manageLogLevelsUseCase.setLevel(
                request.logger(), request.level(), request.ttlMinutes(), principal.getName());
        return new ResponseEntity<>(override, HttpStatus.OK);
    }

    @DeleteMapping("/{logger}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<Void> resetLevel(@PathVariable("logger") String logger, Principal principal) {
        manageLogLevelsUseCase.resetLevel(logger, principal.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
