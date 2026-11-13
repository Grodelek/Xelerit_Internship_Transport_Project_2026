package com.internship.task.ride_together.service;

import com.internship.task.ride_together.dto.OfficeRequest;
import com.internship.task.ride_together.dto.OfficeResponse;
import com.internship.task.ride_together.entity.Office;
import com.internship.task.ride_together.entity.User;
import com.internship.task.ride_together.exception.ResourceNotFoundException;
import com.internship.task.ride_together.repository.OfficeRepository;
import com.internship.task.ride_together.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;

    public OfficeService(OfficeRepository officeRepository, UserRepository userRepository) {
        this.officeRepository = officeRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public void joinOffice(String organisationCode, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist"));
        Office office = officeRepository.findByOrganisationCode(organisationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Office with this organisation code does not exist"));
        user.joinOffice(office);
    }

    public OfficeResponse getOfficeOfUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist"));
        Office office = user.getOffice();
        if (office == null) {
            throw new ResourceNotFoundException("User does not belong to any office");
        }
        return OfficeResponse.from(office);
    }

    private String generateOrganisationCode() {
        String code;
        do {
            code = String.format("%06d", secureRandom.nextInt(1_000_000));
        } while (officeRepository.existsByOrganisationCode(code));
        return code;
    }
}
