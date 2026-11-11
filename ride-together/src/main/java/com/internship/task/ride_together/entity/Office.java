package com.internship.task.ride_together.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "office")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Office {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank
	@Size(max = 100)
	@Column(nullable = false, length = 100)
	private String name;

	@DecimalMin("-90.0")
	@DecimalMax("90.0")
	@Column(nullable = false)
	private double latitude;

	@DecimalMin("-180.0")
	@DecimalMax("180.0")
	@Column(nullable = false)
	private double longitude;

	@NotBlank
	@Size(min = 6, max = 6)
	@Column(nullable = false, unique = true, length = 6)
	private String organisationCode;

	public Office(String name, double latitude, double longitude, String organisationCode) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.organisationCode = organisationCode;
	}

	public void update(String name, double latitude, double longitude) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}
}
