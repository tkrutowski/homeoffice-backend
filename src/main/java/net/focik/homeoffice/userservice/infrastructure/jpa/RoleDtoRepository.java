package net.focik.homeoffice.userservice.infrastructure.jpa;

import net.focik.homeoffice.userservice.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDtoRepository extends JpaRepository<Role, Long> {
}
