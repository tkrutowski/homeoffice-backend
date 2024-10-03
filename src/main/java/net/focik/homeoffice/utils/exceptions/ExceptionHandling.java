package net.focik.homeoffice.utils.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.NoResultException;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.userservice.domain.exceptions.*;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.DisabledException;
//import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Objects;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class ExceptionHandling implements ErrorController {
    public static final  String METHOD_IS_NOT_ALLOWED = "This request method is not allowed on this endpoint. Please send a '%s' request.";
    public static final  String INTERNAL_SERVER_ERROR_MSG = "An error occurred while processing the request.";
    public static final  String ERROR_PROCESSING_FILE = "Error occurred while processing file.";
    public static final  String NOT_ENOUGH_PERMISSION = "Nie masz wystarczających uprawnień.";
    public final String ACCOUNT_LOCKED = "Twoje konto zostało zablokowane. Skontaktuj się z administratorm.";
    public final String INCORRECT_CREDENTIALS = "Niepoprawne dane logowania. Spróbuj ponownie.";
    public final String ACCOUNT_DISABLED = "Twoje konto zostało wyłączone. Jeśli to błąd skontaktuj się z administratorem.";
    public static final  String ERROR_PATH = "/error";


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpResponse> accessDeniedException(AccessDeniedException exception) {
        log.error("Access denied: " + exception.getMessage(), exception);
        return createHttpResponse(FORBIDDEN, NOT_ENOUGH_PERMISSION);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<HttpResponse> badCredentials(BadCredentialsException exception) {
        log.error("Bad credentials: " + exception.getMessage(), exception);
        return createHttpResponse(UNAUTHORIZED, INCORRECT_CREDENTIALS);
    }

    @ExceptionHandler(ObjectAlreadyExistException.class)
    public ResponseEntity<HttpResponse> alreadyExistException(ObjectAlreadyExistException exception) {
        log.error("Object already exists: " + exception.getMessage(), exception);
        return createHttpResponse(CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<HttpResponse> notFoundException(ObjectNotFoundException exception) {
        log.error("Object not found: " + exception.getMessage(), exception);
        return createHttpResponse(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ObjectCanNotBeDeletedException.class)
    public ResponseEntity<HttpResponse> canNotBeDeletedException(ObjectCanNotBeDeletedException exception) {
        log.error("Object can not be deleted: " + exception.getMessage(), exception);
        return createHttpResponse(LOCKED, exception.getMessage());
    }

    @ExceptionHandler(ObjectNotValidException.class)
    public ResponseEntity<HttpResponse> notValidException(ObjectNotValidException exception) {
        log.error("Object not valid: " + exception.getMessage(), exception);
        return createHttpResponse(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<HttpResponse> methodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        log.error("Method not supported", exception);
        HttpMethod supportedMethod = Objects.requireNonNull(exception.getSupportedHttpMethods()).iterator().next();
        return createHttpResponse(METHOD_NOT_ALLOWED, String.format(METHOD_IS_NOT_ALLOWED, supportedMethod));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponse> internalServerErrorException(Exception exception) {
        log.error("Internal server error: " + exception.getMessage(), exception);
        return createHttpResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MSG);
    }

    @ExceptionHandler(NoResultException.class)
    public ResponseEntity<HttpResponse> notFoundException(NoResultException exception) {
        log.error("No result found: " + exception.getMessage(), exception);
        return createHttpResponse(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<HttpResponse> iOException(IOException exception) {
        log.error("I/O error", exception);
        return createHttpResponse(INTERNAL_SERVER_ERROR, ERROR_PROCESSING_FILE);
    }

    @RequestMapping(ERROR_PATH)
    public ResponseEntity<HttpResponse> notFound404(Exception exception) {
        log.error("Not found: " + exception.getMessage(), exception);
        return createHttpResponse(NOT_FOUND, "There is no mapping for this URL");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<HttpResponse> runtimeException(RuntimeException exception) {
        log.error("RuntimeException: " + exception.getMessage(), exception);
//        return createHttpResponse(INTERNAL_SERVER_ERROR, ERROR_PROCESSING_FILE);
        return createHttpResponse(INTERNAL_SERVER_ERROR, exception.getMessage());
    }


    private ResponseEntity<HttpResponse> createHttpResponse(HttpStatus httpStatus, String message) {
        HttpResponse httpResponse = new HttpResponse(httpStatus.value(), httpStatus,
                httpStatus.getReasonPhrase().toUpperCase(), message);

        return new ResponseEntity<>(httpResponse, httpStatus);
    }

//    @ExceptionHandler(DisabledException.class)
//    public ResponseEntity<HttpResponse> accountDisabledException(DisabledException e) {//można przekazać Exception
//        return createHttpResponse(HttpStatus.BAD_REQUEST, ACCOUNT_DISABLED);
//    }
//
//
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<HttpResponse> badCredentialsException() {
//        return createHttpResponse(BAD_REQUEST, INCORRECT_CREDENTIALS);
//    }
//
//
//
//    @ExceptionHandler(LockedException.class)
//    public ResponseEntity<HttpResponse> lockedException() {
//        return createHttpResponse(UNAUTHORIZED, ACCOUNT_LOCKED);
//    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<HttpResponse> tokenExpired(ExpiredJwtException exception) {
        log.error("Token expired", exception);
        return createHttpResponse(UNAUTHORIZED, "Token expired");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<HttpResponse> emailExistException(EmailAlreadyExistsException exception) {
        log.error("Email already exists", exception);
        return createHttpResponse(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<HttpResponse> usernameExistException(UserAlreadyExistsException exception) {
        log.error("Username already exists", exception);
        return createHttpResponse(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<HttpResponse> emailNotFoundException(EmailNotFoundException exception) {
        log.error("Email not found", exception);
        return createHttpResponse(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<HttpResponse> userNotFoundException(UserNotFoundException exception) {
        log.error("User not found", exception);
        return createHttpResponse(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PrivilegeNotFoundException.class)
    public ResponseEntity<HttpResponse> privilegeNotFoundException(PrivilegeNotFoundException exception) {
        log.error("Privilege not found", exception);
        return createHttpResponse(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PasswordNotFoundException.class)
    public ResponseEntity<HttpResponse> passwordNotFoundException(PasswordNotFoundException exception) {
        log.error("Password not found", exception);
        return createHttpResponse(BAD_REQUEST, exception.getMessage());
    }



    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<HttpResponse> authenticationException(AuthenticationException exception) {
        log.error("Authentication error", exception);
        return createHttpResponse(UNAUTHORIZED, exception.getMessage());
    }











    public String getErrorPath() {
        return ERROR_PATH;
    }
}
