package com.internship.task.ride_together.controller;

import com.internship.task.ride_together.dto.UserResponse;
import com.internship.task.ride_together.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		return userService.getProfile(authentication.getName());
	}
}
