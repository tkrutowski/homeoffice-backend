package net.focik.homeoffice.userservice.application;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.Privilege;
import net.focik.homeoffice.userservice.domain.Role;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.userservice.domain.exceptions.PrivilegeNotFoundException;
import net.focik.homeoffice.userservice.domain.port.primary.IAddRoleToUserUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.IChangePrivilegeInUserRoleUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.IDeleteUsersRoleUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.IGetUserRolesUseCase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class UserRolesAppService implements IGetUserRolesUseCase, IAddRoleToUserUseCase,
        IChangePrivilegeInUserRoleUseCase, IDeleteUsersRoleUseCase {

    private final UserFacade userFacade;

    @Override
    public List<Role> getUserRoles(Long idUser) {
        List<Role> rolesList = new ArrayList<>();
        AppUser user = userFacade.findUserById(idUser);
        user.getPrivileges().forEach(privilege -> rolesList.add(privilege.getRole()));

        return rolesList;
    }

    @Override
    public List<Role> getRoles() {
        return userFacade.getAllRoles();
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
    public void changePrivilegesInUserRole(Long idUser, Long idRole, Map<String, String> privilegesToAdd) {
        if (privilegesToAdd == null || privilegesToAdd.isEmpty())
            throw new PrivilegeNotFoundException("Lista przywilejów nie może bć pusta.");
        userFacade.changePrivilegesInUserRole(idUser, idRole, privilegesToAdd);
    }

    @Override
    public void deleteUsersRoleById(Long id, Long idRole) {
        userFacade.deleteUsersRoleById(id, idRole);
    }
}
