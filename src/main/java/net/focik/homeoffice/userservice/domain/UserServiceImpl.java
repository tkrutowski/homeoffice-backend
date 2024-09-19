package net.focik.homeoffice.userservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.domain.exceptions.EmailAlreadyExistsException;
import net.focik.homeoffice.userservice.domain.exceptions.PasswordNotFoundException;
import net.focik.homeoffice.userservice.domain.exceptions.UserAlreadyExistsException;
import net.focik.homeoffice.userservice.domain.exceptions.UserNotFoundException;
import net.focik.homeoffice.userservice.domain.port.primary.IUserService;
import net.focik.homeoffice.userservice.domain.port.secondary.IAppUserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.Date;
import java.util.List;

import static net.focik.homeoffice.userservice.domain.security.constant.UserConstant.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements IUserService {
    private final IAppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    public AppUser addNewUser(String firstName, String lastName, String username, String password, String email, boolean enabled,
                              boolean isNotLocked) throws UserNotFoundException, UserAlreadyExistsException, EmailAlreadyExistsException {
        validateNewUsernameAndEmail(0L, username, email);
        AppUser user = new AppUser();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setEnabled(enabled);
        user.setNotLocked(isNotLocked);
//        user.setRole(ROLE_USER.name());
//        user.setAuthorities(ROLE_USER.getAuthorities());
        userRepository.save(user);
        return user;
    }

    @Override
    public AppUser updateUser(Long id, String newFirstName, String newLastName, String newUsername, String newEmail) {
        AppUser currentUser = validateNewUsernameAndEmail(id, newUsername, newEmail);
        currentUser.setFirstName(newFirstName);
        currentUser.setLastName(newLastName);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        userRepository.save(currentUser);
        return currentUser;
    }

    @Override
    public List<AppUser> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteUser(id);
    }

    @Override
    public void changePassword(Long idUser, String currentPassword, String newPassword) {
        AppUser userById = findUserById(idUser);
        if (userById == null) {
            throw new UserNotFoundException(NO_USER_FOUND_BY_ID + idUser);
        }

        if (userById.getPassword().equals(encodePassword(currentPassword))) {
            userById.setPassword(encodePassword(newPassword));
            userRepository.save(userById);
        } else {
            throw new PasswordNotFoundException(PASSWORD_NOT_FOUND);
        }
    }

    @Override
    public AppUser saveUser(AppUser user) {
        return userRepository.save(user);
    }

    @Override
    public void updateIsActive(Long id, boolean isActive) {
        AppUser userById = findUserById(id);
        if (userById == null)
            throw new UserNotFoundException("Nie znaleziono użytkownika o ID:" + id);

        userById.setEnabled(isActive);

        saveUser(userById);
    }

    @Override
    public void updateIsLock(Long id, boolean isLock) {
        AppUser userById = findUserById(id);
        if (userById == null)
            throw new UserNotFoundException("Nie znaleziono użytkownika o ID:" + id);

        userById.setNotLocked(isLock);

        saveUser(userById);
    }

    @Override
    public AppUser findUserByUsername(String username) {
        AppUser userByUsername = userRepository.findUserByUsername(username);
        //TODO przenieść do servisu
//        if(userByUsername == null)
//            throw new UserNotFoundException("Nie znaleziono użytkownika o username:"+ username);
        return userByUsername;
    }


    @Override
    public AppUser findUserById(Long id) {
        AppUser userById = userRepository.findUserById(id);
        //TODO przenieść do servisu
//        if(userById == null)
//            throw new UserNotFoundException("Nie znaleziono użytkownika o ID:"+ id);
        return userById;
    }


    @Override
    public AppUser findUserByEmail(String email) {
        AppUser userByEmail = userRepository.findUserByEmail(email);
        //TODO przenieść do servisu
//        if(userByEmail == null)
//            throw new UserNotFoundException("Nie znaleziono użytkownika o email:"+ email);
        return userByEmail;
    }

    private AppUser validateNewUsernameAndEmail(Long currentId, String newUsername, String newEmail) throws UserNotFoundException, UserAlreadyExistsException, EmailAlreadyExistsException {
        AppUser userByNewUsername = findUserByUsername(newUsername);
        AppUser userByNewEmail = findUserByEmail(newEmail);
        if (currentId > 0) {
            AppUser currentUser = findUserById(currentId);
            if (currentUser == null) {
                throw new UserNotFoundException(NO_USER_FOUND_BY_ID + currentId);
            }
            if (userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())) {
                throw new UserAlreadyExistsException(USERNAME_ALREADY_EXISTS);
            }
            if (userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())) {
                throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        } else {
            if (userByNewUsername != null) {
                throw new UserAlreadyExistsException(USERNAME_ALREADY_EXISTS);
            }
            if (userByNewEmail != null) {
                throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}
