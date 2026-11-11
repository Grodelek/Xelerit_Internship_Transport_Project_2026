package com.internship.task.ride_together.repository;

import com.internship.task.ride_together.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OfficeRepository extends JpaRepository<Office, UUID> {

	Optional<Office> findByName(String name);

	boolean existsByOrganisationCode(String organisationCode);
}
