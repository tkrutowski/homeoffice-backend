package net.focik.homeoffice.userservice.application;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.userservice.domain.port.primary.*;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserAppService implements GetUserUseCase, IAddNewUserUseCase, IUpdateUserUseCase,
        IDeleteUserUseCase, IChangePasswordUseCase {

    private final UserFacade userFacade;

    @Override
    public AppUser findUserByUsername(String username) {
        return userFacade.findUserByUsername(username);
    }

    @Override
    public AppUser findUserById(Long id) {
        return userFacade.findUserById(id);
    }

    @Override
    public List<AppUser> getAllUsers() {
        return userFacade.getAllUsers();
    }

    @Override
    public AppUser addNewUser(String firstName, String lastName, String username, String password, String email, boolean enabled,
                              boolean isNotLocked) {
        return userFacade.registerUser(firstName, lastName, username, password, email, enabled, isNotLocked);
    }

    @Override
    public AppUser updateUser(Long id, String firstName, String lastName, String username, String email) {
        return userFacade.updateUser(id, firstName, lastName, username, email);
    }

    @Override
    public void updateIsActive(Long id, boolean isActive) {
        userFacade.updateIsActive(id, isActive);
    }

    @Override
    public void updateIsLock(Long id, boolean isLock) {
        userFacade.updateIsLock(id, isLock);
    }

    @Override
    public void deleteUserById(Long id) {
        userFacade.deleteUser(id);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        userFacade.changePassword(id, oldPassword, newPassword);
    }
}
