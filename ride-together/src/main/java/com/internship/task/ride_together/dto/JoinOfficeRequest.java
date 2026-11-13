package com.internship.task.ride_together.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinOfficeRequest(
        @NotBlank @Pattern(regexp = "\\d{6}") String organisationCode
) {
}