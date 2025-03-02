package net.focik.homeoffice.userservice.domain.exceptions;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }

}
