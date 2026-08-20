package com.portfoliotracker.backend.security;

import java.util.Locale;

import com.portfoliotracker.backend.user.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public Long currentUserId() {
		String email = currentUserEmail();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found."))
				.getId();
	}

	private static String currentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new AccessDeniedException("Authentication is required.");
		}
		return authentication.getName().trim().toLowerCase(Locale.ROOT);
	}
}
