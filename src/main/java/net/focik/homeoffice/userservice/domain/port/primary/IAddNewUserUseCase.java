package net.focik.homeoffice.userservice.domain.port.primary;


import net.focik.homeoffice.userservice.domain.AppUser;

public interface IAddNewUserUseCase {

    AppUser addNewUser(String firstName, String lastName, String username, String password, String email, boolean enabled,
                       boolean isNotLocked);
}
