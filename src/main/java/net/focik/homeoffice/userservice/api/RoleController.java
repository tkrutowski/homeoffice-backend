package net.focik.homeoffice.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.userservice.api.dto.PrivilegeDto;
import net.focik.homeoffice.userservice.api.dto.RoleDto;
import net.focik.homeoffice.userservice.domain.Privilege;
import net.focik.homeoffice.userservice.domain.Role;
import net.focik.homeoffice.userservice.domain.port.primary.AddPrivilegeUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.DeletePrivilegeUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.GetPrivilegesUseCase;
import net.focik.homeoffice.userservice.domain.port.primary.UpdatePrivilegeUseCase;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

//@CrossOrigin(exposedHeaders = "Jwt-Token")
@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(path = {"/api/v1/user/role"})
//najpierw sprawdza czy jest jakiś exception handler w exceptionHandling
public class RoleController {

    private final ModelMapper mapper;
    private final GetPrivilegesUseCase getPrivilegesUseCase;
    private final AddPrivilegeUseCase addPrivilegeUseCase;
    private final UpdatePrivilegeUseCase updatePrivilegeUseCase;
    private final DeletePrivilegeUseCase deletePrivilegeUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    ResponseEntity<List<PrivilegeDto>> getUserRoles(@PathVariable Long id) {
        List<Privilege> userPrivileges = getPrivilegesUseCase.getUserPrivileges(id);
        List<PrivilegeDto> privilegesDtos = userPrivileges.stream()
                .map(role -> mapper.map(role, PrivilegeDto.class))
                .peek(privilegeDto -> privilegeDto.setIdUser(id))
                .toList();
        return new ResponseEntity<>(privilegesDtos, HttpStatus.OK);
    }

    @PutMapping()
    public ResponseEntity<HttpResponse> updatePrivilegesInUserRole(@RequestBody PrivilegeDto privilegeDto) {
        updatePrivilegeUseCase.updatePrivilegesInUserRole(privilegeDto.getIdUser(), mapper.map(privilegeDto, Privilege.class));
        return response("Zaaktualizowano uprawnienia użytkownika.");
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    ResponseEntity<List<RoleDto>> getRoles() {
        List<Role> allRoles = getPrivilegesUseCase.getRoles();
        List<RoleDto> roleDtos = allRoles.stream().map(role -> mapper.map(role, RoleDto.class)).collect(Collectors.toList());
        return new ResponseEntity<>(roleDtos, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> addRoleToUser(@RequestParam("userID") Long idUser, @RequestParam("roleID") Long idRole) {
        addPrivilegeUseCase.addRoleToUser(idUser, idRole);
        return response("Dodano role do użytkownika.");
    }

    @DeleteMapping()
    public ResponseEntity<HttpResponse> deleteRoleFromUser(@RequestParam("userID") Long idUser, @RequestParam("roleID") Long idRole) {
        deletePrivilegeUseCase.deleteUsersRoleById(idUser, idRole);
        return response("Role has been deleted from user.");
    }

    private ResponseEntity<HttpResponse> response(String message) {
        HttpResponse body = new HttpResponse(HttpStatus.OK.value(), HttpStatus.OK, HttpStatus.OK.getReasonPhrase(), message);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
