package com.vcube.stockwise.api;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) ProblemDetail notFoundOrInvalid(IllegalArgumentException e){var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage());p.setTitle("Invalid request");return p;}
 @ExceptionHandler(MethodArgumentNotValidException.class) ProblemDetail validation(MethodArgumentNotValidException e){var detail=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).reduce((a,b)->a+"; "+b).orElse("Request validation failed");var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,detail);p.setTitle("Validation failed");return p;}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ProblemDetail> forbidden(AccessDeniedException e){var p=ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,"You do not have permission to perform this action");p.setTitle("Forbidden");return ResponseEntity.status(HttpStatus.FORBIDDEN).body(p);}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ProblemDetail> status(ResponseStatusException e){var p=ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"The request could not be completed":e.getReason());p.setTitle("Request could not be completed");return ResponseEntity.status(e.getStatusCode()).body(p);}
 @ExceptionHandler(Exception.class) ResponseEntity<ProblemDetail> unexpected(Exception e){var p=ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"The server could not complete the request. Try again shortly.");p.setTitle("Server error");return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(p);}
}
