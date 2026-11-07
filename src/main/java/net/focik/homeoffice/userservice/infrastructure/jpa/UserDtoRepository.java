package net.focik.homeoffice.userservice.infrastructure.jpa;

import net.focik.homeoffice.userservice.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserDtoRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

}
