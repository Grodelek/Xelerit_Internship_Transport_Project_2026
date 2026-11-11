package com.internship.task.ride_together.dto;

import com.internship.task.ride_together.entity.Office;

import java.util.UUID;

public record OfficeResponse(UUID id, String name, double latitude, double longitude, String organisationCode) {

	public static OfficeResponse from(Office office) {
		return new OfficeResponse(office.getId(), office.getName(), office.getLatitude(),
				office.getLongitude(), office.getOrganisationCode());
	}
}
