package net.focik.homeoffice.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.api.dto.UserDto;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.exceptions.EmailAlreadyExistsException;
import net.focik.homeoffice.userservice.domain.exceptions.UserAlreadyExistsException;
import net.focik.homeoffice.userservice.domain.exceptions.UserNotFoundException;
import net.focik.homeoffice.userservice.domain.port.primary.*;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = {"/api/v1/user"})

public class UserController extends ExceptionHandling {

    private final ModelMapper mapper;
    private final GetUserUseCase getUserUseCase;
    private final IAddNewUserUseCase addNewUserUseCase;
    private final IUpdateUserUseCase updateUserUseCase;
    private final IDeleteUserUseCase deleteUserUseCase;
    private final IChangePasswordUseCase changePasswordUseCase;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ResponseEntity<AppUser> getUser(@PathVariable Long id) {
        log.info("Try find user by ID: {}",id);
        AppUser user = getUserUseCase.findUserById(id);
        if (user == null) {
            log.warn("User not found with ID: {}",id);
        }else {
            log.info("Found user with ID: {}", id);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping
    @Secured("ROLE_ADMIN")
    ResponseEntity<List<UserDto>> getUsers( @RequestHeader(name = AUTHORITIES, required = false) String[] roles) {
        log.info("Try find all users");
        List<AppUser> allUsers = getUserUseCase.getAllUsers();
        if (allUsers.isEmpty()) {
            log.warn("Users not found");
        }else {
            log.info("Found {} users", allUsers.size());
        }
        List<UserDto> userDtos = allUsers.stream()
                .map(user -> mapper.map(user, UserDto.class))
                .toList();
        return new ResponseEntity<>(userDtos, HttpStatus.OK);
    }

    @PostMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<AppUser> register(@RequestBody AppUser user) throws UserNotFoundException, UserAlreadyExistsException, EmailAlreadyExistsException {
        AppUser newUser = addNewUserUseCase.addNewUser(user.getFirstName(), user.getLastName(), user.getUsername(), user.getPassword(),
                user.getEmail(), user.isEnabled(), user.isNotLocked());
        return new ResponseEntity<>(newUser, CREATED);
    }

    @PutMapping("/update")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<AppUser> updateUser(@RequestBody AppUser user) {
        AppUser updatedUser = updateUserUseCase.updateUser(user.getId(), user.getFirstName(),
                user.getLastName(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(updatedUser, OK);
    }

    @PutMapping("/update/active/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<HttpResponse> updateUserActive(@PathVariable Long id, @RequestParam("enabled") boolean isEnabled) {
        updateUserUseCase.updateIsActive(id, isEnabled);
        return response(HttpStatus.OK, "Zaaktualizowano status użytkownika.");
    }

    @PutMapping("/update/lock/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<HttpResponse> updateUserLock(@PathVariable Long id, @RequestParam("lock") boolean isLock) {
        updateUserUseCase.updateIsLock(id, isLock);
        return response(HttpStatus.OK, "Zaaktualizowano status użytkownika.");
    }

    @PutMapping("/changepass/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<HttpResponse> changePassword(@PathVariable Long id, @RequestParam("oldPass") String oldPass, @RequestParam("newPass") String newPass) {
        changePasswordUseCase.changePassword(id, oldPass, newPass);
        return response(HttpStatus.OK, "Hasło zmienione.");
    }


    @DeleteMapping("/delete/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable Long id) {
        deleteUserUseCase.deleteUserById(id);
        return response(HttpStatus.NO_CONTENT, "Użytkownik usunięty.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}
