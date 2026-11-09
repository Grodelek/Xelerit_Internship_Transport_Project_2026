package com.internship.task.ride_together.service;

import com.internship.task.ride_together.dto.LoginRequest;
import com.internship.task.ride_together.dto.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserService userService;

	public Authentication authenticate(LoginRequest request) {
		return authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
	}

	public UserResponse profileOf(Authentication authentication) {
		return userService.getProfile(authentication.getName());
	}
}
