package net.focik.homeoffice.userservice.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.Role;
import net.focik.homeoffice.userservice.domain.port.secondary.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {
    private final IRoleDtoRepository roleDtoRepository;

    @Override
    public Role addRole(Role role) {
        return null;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleDtoRepository.findAll();
    }

    @Override
    public Role getRoleById(Long id) {
        Optional<Role> byId = roleDtoRepository.findById(id);

        if (byId.isEmpty())
            return null;

        return byId.get();
    }
}
