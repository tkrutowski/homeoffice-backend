package net.focik.homeoffice.userservice.domain.port.primary;

import net.focik.homeoffice.userservice.domain.AppUser;

import java.util.List;

public interface GetUserUseCase {
    AppUser findUserByUsername(String username);

    AppUser findUserById(Long id);

    AppUser findUserByEmail(String email);

    List<AppUser> getAllUsers();

}
