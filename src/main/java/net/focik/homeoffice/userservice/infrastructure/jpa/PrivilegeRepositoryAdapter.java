package net.focik.homeoffice.userservice.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.Privilege;
import net.focik.homeoffice.userservice.domain.port.secondary.IPrivilegeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PrivilegeRepositoryAdapter implements IPrivilegeRepository {
    private final IPrivilegeDtoRepository privilegeDtoRepository;


    @Override
    public Privilege getPrivilegeById(Long id) {
        Optional<Privilege> byId = privilegeDtoRepository.findById(id);

        if (byId.isEmpty())
            return null;

        return byId.get();
    }

    @Override
    public Privilege save(Privilege privilege) {
        return privilegeDtoRepository.save(privilege);
    }
}
