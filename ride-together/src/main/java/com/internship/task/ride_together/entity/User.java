package com.internship.task.ride_together.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank
	@Size(min = 3, max = 30)
	@Column(nullable = false, unique = true, length = 30)
	private String username;

	@NotBlank
	@Email
	@Column(nullable = false, unique = true)
	private String email;

	@NotBlank
	@Column(nullable = false)
	private String password;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private boolean carOwner;

	@Min(0)
	@Column(nullable = false)
	private int seats;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Role role = Role.USER;

	@DecimalMin("-90.0")
	@DecimalMax("90.0")
	private Double latitude;

	@DecimalMin("-180.0")
	@DecimalMax("180.0")
	private Double longitude;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "office_id")
	private Office office;

	public User(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public void joinOffice(Office office) {
		this.office = office;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}
}
