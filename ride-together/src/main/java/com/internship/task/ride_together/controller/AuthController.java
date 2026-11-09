package com.internship.task.ride_together.controller;

import com.internship.task.ride_together.dto.LoginRequest;
import com.internship.task.ride_together.dto.RegisterRequest;
import com.internship.task.ride_together.dto.UserResponse;
import com.internship.task.ride_together.service.AuthService;
import com.internship.task.ride_together.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
	private final UserService userService;
	private final AuthService authService;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return userService.register(request);
	}

	@PostMapping("/login")
	public UserResponse login(@Valid @RequestBody LoginRequest request,
							  HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		Authentication authentication = authService.authenticate(request);
		if (httpRequest.getSession(false) != null) {
			httpRequest.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);
		return authService.profileOf(authentication);
	}
}
