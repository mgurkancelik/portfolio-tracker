package com.portfoliotracker.backend.auth;

import java.util.Locale;

import com.portfoliotracker.backend.security.JwtService;
import com.portfoliotracker.backend.user.User;
import com.portfoliotracker.backend.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.findByEmail(email).isPresent()) {
			throw new UserAlreadyExistsException(email);
		}

		User user = userRepository.save(new User(email, passwordEncoder.encode(request.password())));
		return new TokenResponse(jwtService.generateToken(user.getEmail()));
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		return new TokenResponse(jwtService.generateToken(user.getEmail()));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
