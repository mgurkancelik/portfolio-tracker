package com.portfoliotracker.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

	public UserAlreadyExistsException(String email) {
		super("User already exists for email: %s".formatted(email));
	}
}
