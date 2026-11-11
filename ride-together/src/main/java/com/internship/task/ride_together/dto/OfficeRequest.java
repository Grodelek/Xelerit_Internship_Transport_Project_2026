package com.internship.task.ride_together.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfficeRequest(
		@NotBlank @Size(max = 100) String name,
		@DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
		@DecimalMin("-180.0") @DecimalMax("180.0") double longitude
) {
}
