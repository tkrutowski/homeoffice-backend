package net.focik.homeoffice.userservice.domain.security.constant;

import java.util.regex.Pattern;

public class UserConstant {
    public static final String USERNAME_ALREADY_EXISTS = "Username already exists";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String NO_USER_FOUND_BY_USERNAME = "No user found by username: ";
    public static final String NO_USER_FOUND_BY_ID = "No user found by id: ";
    public static final String PASSWORD_NOT_FOUND = "Password not found. ";
    public static final String WEAK_PASSWORD = "Hasło musi mieć co najmniej 8 znaków, jedną cyfrę i jeden znak specjalny.";
    public static final String FOUND_USER_BY_USERNAME = "Returning found user by username: ";
    //wspoldzielona przez zmiane hasla (UserServiceImpl) i reset hasla (PasswordResetFacade)
    public static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    public static final String INVALID_RESET_TOKEN = "Link do resetu hasła jest nieprawidłowy lub wygasł.";
}
