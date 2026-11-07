package net.focik.homeoffice.userservice.infrastructure.jpa;

import net.focik.homeoffice.userservice.domain.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;

interface PrivilegeDtoRepository extends JpaRepository<Privilege, Long> {
//    Optional<Privilege> findByName(String name);
}
