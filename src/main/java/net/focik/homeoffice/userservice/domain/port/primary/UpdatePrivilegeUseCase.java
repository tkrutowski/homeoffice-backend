package net.focik.homeoffice.userservice.domain.port.primary;


import net.focik.homeoffice.userservice.domain.Privilege;

public interface UpdatePrivilegeUseCase {
    void updatePrivilegesInUserRole(Long idUser, Privilege privilege);
}
