package tn.driveeasy.location;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail badRequest(Exception ex) {
        String detail = ex instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + " " + e.getDefaultMessage()).findFirst().orElse("Requête invalide.")
                : ex.getMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
    @ExceptionHandler(RemoteServiceException.class)
    ProblemDetail remote(RemoteServiceException ex) { return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()); }
}
