package com.smartagro.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.smartagro.backend.user.AppUser;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final Duration expiration;

	public JwtTokenService(
			JwtEncoder jwtEncoder,
			@Value("${app.jwt.expiration:PT1H}") Duration expiration) {
		if (expiration.isZero() || expiration.isNegative()) {
			throw new IllegalArgumentException("JWT expiration must be a positive duration.");
		}
		this.jwtEncoder = jwtEncoder;
		this.expiration = expiration;
	}

	public String createToken(AppUser user) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("smart-agro")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(expiration))
				.subject(user.getEmail())
				.claim("userId", user.getId())
				.claim("roles", List.of(user.getRole().name()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long getExpirationSeconds() {
		return expiration.toSeconds();
	}
}
