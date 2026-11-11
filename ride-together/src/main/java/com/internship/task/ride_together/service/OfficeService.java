package com.internship.task.ride_together.service;

import com.internship.task.ride_together.dto.OfficeRequest;
import com.internship.task.ride_together.dto.OfficeResponse;
import com.internship.task.ride_together.entity.Office;
import com.internship.task.ride_together.exception.ResourceNotFoundException;
import com.internship.task.ride_together.repository.OfficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OfficeService(OfficeRepository officeRepository) {
        this.officeRepository = officeRepository;
    }

    private Office getOffice(String name) {
        Optional<Office> officeOptional = officeRepository.findByName(name);
        if (officeOptional.isEmpty()) {
            throw new ResourceNotFoundException("Office " + name + " not found");
        }
        return officeOptional.get();
    }

    @Transactional
    public OfficeResponse createOffice(OfficeRequest officeRequest) {
        Office office = new Office(officeRequest.name(), officeRequest.latitude(),
                officeRequest.longitude(), generateOrganisationCode());
        return OfficeResponse.from(officeRepository.save(office));
    }

    private String generateOrganisationCode() {
        String code;
        do {
            code = String.format("%06d", secureRandom.nextInt(1_000_000));
        } while (officeRepository.existsByOrganisationCode(code));
        return code;
    }
}
