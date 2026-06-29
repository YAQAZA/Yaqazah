package com.yaqazah.common.config;


import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@NullMarked
@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgument(
            IllegalArgumentException e
    ){
        return ResponseEntity.badRequest()
                .body(Map.of("error",e.getMessage()));
    }




    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalState(
            IllegalStateException e
    ){
        return ResponseEntity.badRequest()
                .body(Map.of("error",e.getMessage()));
    }




    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> security(
            SecurityException e
    ){
        return ResponseEntity.status(403)
                .body(Map.of("error",e.getMessage()));
    }




    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badCredentials(
            BadCredentialsException e
    ){
        return ResponseEntity.status(401)
                .body(
                        Map.of(
                                "error",
                                "Invalid credentials"
                        )
                );
    }





    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> userNotFound(
            UsernameNotFoundException e
    ){
        return ResponseEntity.status(401)
                .body(Map.of("error",e.getMessage()));
    }




    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(
            MethodArgumentNotValidException e
    ){

        return ResponseEntity.badRequest()
                .body(
                        Map.of(
                                "error",
                                "Validation failed"
                        )
                );
    }





    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> general(
            Exception e
    ){

        e.printStackTrace();

        return ResponseEntity.status(500)
                .body(
                        Map.of(
                                "error",
                                "Internal server error"
                        )
                );
    }
}