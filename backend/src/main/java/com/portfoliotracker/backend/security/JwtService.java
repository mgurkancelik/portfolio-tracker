package com.portfoliotracker.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

	private final SecretKey signingKey;

	private final Duration tokenExpiration;

	public JwtService(
			@Value("${jwt.secret:}") String jwtSecret,
			@Value("${jwt.expiration:PT2H}") Duration tokenExpiration) {
		this.signingKey = resolveSigningKey(jwtSecret);
		this.tokenExpiration = tokenExpiration;
	}

	public String generateToken(String email) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(tokenExpiration)))
				.signWith(signingKey)
				.compact();
	}

	public String extractEmail(String token) {
		return extractClaims(token).getSubject();
	}

	public boolean isTokenValid(String token) {
		try {
			extractClaims(token);
			return true;
		}
		catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims extractClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private static SecretKey resolveSigningKey(String jwtSecret) {
		if (!StringUtils.hasText(jwtSecret)) {
			return Jwts.SIG.HS256.key().build();
		}
		return Keys.hmacShaKeyFor(sha256(jwtSecret));
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
		}
	}
}
