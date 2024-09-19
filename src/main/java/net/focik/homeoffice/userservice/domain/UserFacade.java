package net.focik.homeoffice.userservice.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.port.primary.IUserService;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final IUserService userService;
    private final RoleService roleService;

    public AppUser registerUser(String firstName, String lastName, String username, String password,
                                String email, boolean enabled, boolean isNotLocked) {
        return userService.addNewUser(firstName, lastName, username, password, email, enabled, isNotLocked);
    }

    public AppUser findUserByUsername(String username) {
        return userService.findUserByUsername(username);
    }

    public AppUser updateUser(Long id, String firstName, String lastName, String username, String email) {
        return userService.updateUser(id, firstName, lastName, username, email);
    }

    public void deleteUser(Long id) {
        userService.deleteUser(id);
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        userService.changePassword(id, oldPassword, newPassword);
    }

    public AppUser findUserById(Long id) {
        return userService.findUserById(id);
    }

    public List<AppUser> getAllUsers() {
        return userService.getUsers();
    }

    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @Transactional
    public void addRoleToUser(Long idUser, Long idRole) {
        AppUser userWithNewRole = roleService.addRoleToUser(userService.findUserById(idUser), idRole);
        userService.saveUser(userWithNewRole);
    }

//    public Privilege findPrivilegeByName(String name){
//        return roleService.findPrivilegeByName(name);
//    }

    public void changePrivilegesInUserRole(Long idUser, Long idRole, Map<String, String> privilegeMap) {
        AppUser userById = userService.findUserById(idUser);
        boolean result = roleService.changePrivilegesInUserRole(userById, idRole, privilegeMap);

        if (result)
            userService.saveUser(userById);
    }

    public void deleteUsersRoleById(Long idUser, Long idRole) {
        AppUser userById = userService.findUserById(idUser);
        roleService.deleteRoleFromUser(userById, idRole);

        userService.saveUser(userById);
    }

    public Privilege getRoleDetails(Long idUser, Long idRole) {
        AppUser userById = userService.findUserById(idUser);
        return roleService.getRoleDetails(userById, idRole);
    }

    public void updateIsActive(Long id, boolean isActive) {
        userService.updateIsActive(id, isActive);
    }

    public void updateIsLock(Long id, boolean isLock) {
        userService.updateIsLock(id, isLock);
    }
}
