package net.focik.homeoffice.userservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.domain.exceptions.RoleNotFoundException;
import net.focik.homeoffice.userservice.domain.port.secondary.IPrivilegeRepository;
import net.focik.homeoffice.userservice.domain.port.secondary.IRoleRepository;
import net.focik.homeoffice.utils.share.PrivilegeType;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    private final IRoleRepository roleRepository;
    private final IPrivilegeRepository privilegeRepository;

    public List<Role> getAllRoles() {

        return roleRepository.getAllRoles();
    }

    public Role findRoleById(Long idRole) {

        Role roleById = roleRepository.getRoleById(idRole);

        if (roleById == null)
            throw new RoleNotFoundException("Role not found by id: " + idRole);
        return roleById;
    }

    public AppUser addRoleToUser(AppUser user, Long idRole) {

        Role roleById = roleRepository.getRoleById(idRole);

        if (roleById == null)
            throw new RoleNotFoundException("Role not found by id: " + idRole);
        Privilege privilege = new Privilege();
        privilege.setRead(PrivilegeType.NULL);
        privilege.setWrite(PrivilegeType.NULL);
        privilege.setDelete(PrivilegeType.NULL);
        privilege.setRole(roleById);

//        Privilege save = privilegeRepository.save(privilege);

        user.addPrivilege(privilege);

        return user;
    }

    public boolean changePrivilegesInUserRole(AppUser user, Long idRole, Map<String, String> privilegeList) {
        Privilege privilegeByRoleID = getPrivilegeByRoleID(user.getPrivileges(), idRole);

        for (Map.Entry<String, String> next : privilegeList.entrySet()) {
            switch (next.getKey()) {
                case "read" -> privilegeByRoleID.setRead(PrivilegeType.valueOf(next.getValue()));
                case "write" -> privilegeByRoleID.setWrite(PrivilegeType.valueOf(next.getValue()));
                case "delete" -> privilegeByRoleID.setDelete(PrivilegeType.valueOf(next.getValue()));
            }
        }

        int index = findIndex(privilegeByRoleID.getId(), user.getPrivileges());

        if (index > -1) {
            user.getPrivileges().set(index, privilegeByRoleID);
        }

        return true;
    }

    public void deleteRoleFromUser(AppUser user, Long idRole) {
        Privilege privilege = getPrivilegeByRoleID(user.getPrivileges(), idRole);

        user.deletePrivilege(privilege);
    }

    private int findIndex(Long id, List<Privilege> privileges) {
        int index = -1;
        for (Privilege privilege : privileges) {
            index++;
            if (privilege.getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }


    public Privilege getRoleDetails(AppUser user, Long idRole) {
        return getPrivilegeByRoleID(user.getPrivileges(), idRole);
    }


    private Privilege getPrivilegeByRoleID(List<Privilege> privileges, Long idRole) {
        if (privileges == null || privileges.isEmpty())
            throw new RoleNotFoundException("Role id: " + idRole + " not found");


        Optional<Privilege> optionalPrivilege = privileges.stream()
                .filter(privilege -> privilege.getRole().getId().equals(idRole))
                .findFirst();

        if (optionalPrivilege.isEmpty())
            throw new RoleNotFoundException("Role id: " + idRole + " not found");

        return optionalPrivilege.get();
    }
}
