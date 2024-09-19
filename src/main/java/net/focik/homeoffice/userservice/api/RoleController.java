package net.focik.homeoffice.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.api.dto.PrivilegeDto;
import net.focik.homeoffice.userservice.api.dto.RoleDto;
import net.focik.homeoffice.userservice.domain.Privilege;
import net.focik.homeoffice.userservice.domain.Role;
import net.focik.homeoffice.userservice.domain.port.primary.IAddRoleToUserUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.IChangePrivilegeInUserRoleUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.IDeleteUsersRoleUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.IGetUserRolesUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@CrossOrigin(exposedHeaders = "Jwt-Token")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = {"/api/v1/user/role"})
public class RoleController extends ExceptionHandling {

    private final ModelMapper mapper;
    private final IGetUserRolesUseCase getUserRolesUseCase;
    private final IAddRoleToUserUseCase addRoleToUserUseCase;
    private final IChangePrivilegeInUserRoleUseCase changePrivilegeInUserRoleUseCase;
    private final IDeleteUsersRoleUseCase deleteUsersRoleUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<List<RoleDto>> getUserRoles(@PathVariable Long id) {
        log.debug("Try find roles by idUser: = {}", id);
        List<Role> allRoles = getUserRolesUseCase.getUserRoles(id);
        log.debug("Found {} roles for user with ID: {}",allRoles.size(), id);
        List<RoleDto> roleDtos = allRoles.stream()
                .map(role -> mapper.map(role, RoleDto.class))
                .toList();
        return new ResponseEntity<>(roleDtos, HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<List<RoleDto>> getRoles() {
        List<Role> allRoles = getUserRolesUseCase.getRoles();
        List<RoleDto> roleDtos = allRoles.stream()
                .map(role -> mapper.map(role, RoleDto.class))
                .toList();
        return new ResponseEntity<>(roleDtos, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> addRoleToUser(@RequestParam("userID") Long idUser, @RequestParam("roleID") Long idRole) {
        addRoleToUserUseCase.addRoleToUser(idUser, idRole);
        return response(HttpStatus.OK, "Dodano role do użytkownika.");
    }

    @DeleteMapping()
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteRoleFromUser(@RequestParam("userID") Long idUser, @RequestParam("roleID") Long idRole) {
        deleteUsersRoleUseCase.deleteUsersRoleById(idUser, idRole);
        return response(HttpStatus.OK, "Role has been deleted from user.");
    }


    @GetMapping("/details")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<List<PrivilegeDto>> getRolesDetails(@RequestParam("userID") Long idUser,
                                                       @RequestParam("roleID") Long idRole) {
        List<PrivilegeDto> roleDetails = new ArrayList<>();
        Privilege details = getUserRolesUseCase.getRoleDetails(idUser, idRole);
        roleDetails.add(new PrivilegeDto("read", details.getRead().toString()));
        roleDetails.add(new PrivilegeDto("write", details.getWrite().toString()));
        roleDetails.add(new PrivilegeDto("delete", details.getDelete().toString()));
        return new ResponseEntity<>(roleDetails, HttpStatus.OK);
    }

    @PutMapping("/details")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> addPrivilegesToUserRole(@RequestParam("userID") Long idUser,
                                                                @RequestParam("roleID") Long idRole,
                                                                @RequestBody List<PrivilegeDto> privList) {

        Map<String, String> map = new HashMap<>();
        privList.forEach(privilegeDto -> map.put(privilegeDto.getName(), privilegeDto.getValue()));

        changePrivilegeInUserRoleUseCase.changePrivilegesInUserRole(idUser, idRole, map);
        return response(HttpStatus.OK, "Dodano przywilej do roli użytkownika.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}
