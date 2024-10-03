package net.focik.homeoffice.userservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.domain.exceptions.RoleNotFoundException;
import net.focik.homeoffice.userservice.domain.port.secondary.IPrivilegeRepository;
import net.focik.homeoffice.userservice.domain.port.secondary.RoleRepository;
import net.focik.homeoffice.utils.share.PrivilegeType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    private final RoleRepository roleRepository;

    public List<Role> getAllRoles() {

        return roleRepository.getAllRoles();
    }

    public AppUser addRoleToUser(AppUser user, Long idRole) {

        Role roleById = roleRepository.getRoleById(idRole);

        if(roleById == null)
            throw new RoleNotFoundException("Role not found by id: "+ idRole);
        Privilege privilege = new Privilege();
        privilege.setRead(PrivilegeType.NULL);
        privilege.setWrite(PrivilegeType.NULL);
        privilege.setDelete(PrivilegeType.NULL);
        privilege.setRole(roleById);

        user.addPrivilege(privilege);

        return user;
    }


    public void changePrivilegesInUserRole(AppUser user, Privilege privilege) {
        Privilege privilegeByRoleID = getPrivilegeByRoleID(user.getPrivileges(), privilege.getRole().getId());

        privilegeByRoleID.setRead(privilege.getRead());
        privilegeByRoleID.setWrite(privilege.getWrite());
        privilegeByRoleID.setDelete(privilege.getDelete());
    }

    public void deleteRoleFromUser(AppUser user, Long idRole) {
        Privilege privilege = getPrivilegeByRoleID(user.getPrivileges(), idRole);
        user.deletePrivilege(privilege);
    }


    public Privilege getRoleDetails(AppUser user, Long idRole) {
        return getPrivilegeByRoleID(user.getPrivileges(), idRole);
    }


    private Privilege getPrivilegeByRoleID(List<Privilege> privileges, Long idRole) {
        if(privileges == null || privileges.isEmpty())
            throw new RoleNotFoundException("Role id: "+idRole+" not found");

        Optional<Privilege> optionalPrivilege = privileges.stream()
                .filter(privilege -> privilege.getRole().getId().equals(idRole))
                .findFirst();

        if(optionalPrivilege.isEmpty())
            throw new RoleNotFoundException("Role id: "+idRole+" not found");

        return optionalPrivilege.get();
    }
}
