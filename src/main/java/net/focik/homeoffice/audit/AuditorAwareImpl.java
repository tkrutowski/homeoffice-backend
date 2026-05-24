package net.focik.homeoffice.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }
            String principal = auth.getPrincipal().toString();
            if ("anonymousUser".equals(principal)) {
                return Optional.empty();
            }
            return Optional.of(principal);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
