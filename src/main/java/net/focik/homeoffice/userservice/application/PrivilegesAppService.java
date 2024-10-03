package net.focik.homeoffice.userservice.application;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.Privilege;
import net.focik.homeoffice.userservice.domain.Role;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.userservice.domain.port.primary.AddPrivilegeUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.DeletePrivilegeUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.GetPrivilegesUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.UpdatePrivilegeUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PrivilegesAppService implements GetPrivilegesUseCase, AddPrivilegeUseCase,
        UpdatePrivilegeUseCase, DeletePrivilegeUseCase {

    private final UserFacade userFacade;

    @Override
    public List<Privilege> getUserPrivileges(Long idUser) {
        AppUser user = userFacade.findUserById(idUser);
        return user.getPrivileges();
    }

    @Override
    public List<Role> getRoles() {
        return userFacade.getAllRoles();
    }

    @Override
    public List<Privilege> getPrivilegesByUser(Long idUser) {
        return List.of();
    }

    @Override
    public Privilege getRoleDetails(Long idUser, Long idRole) {
        return userFacade.getRoleDetails(idUser, idRole);
    }

    @Override
    public void addRoleToUser(Long idUser, Long idRole) {
       userFacade.addRoleToUser(idUser, idRole);
    }

    @Override
    public void updatePrivilegesInUserRole(Long idUser, Privilege privilege) {
        userFacade.changePrivilegesInUserRole(idUser, privilege);
    }

    @Override
    public void deleteUsersRoleById(Long id, Long idRole) {
        userFacade.deleteUsersRoleById(id, idRole);
    }
}
