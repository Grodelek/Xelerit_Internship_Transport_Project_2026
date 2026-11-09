package com.internship.task.ride_together.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(min = 3, max = 30) String username,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8) String password
) {

	public RegisterRequest {
		username = username == null ? null : username.trim();
		email = email == null ? null : email.trim();
	}
}
