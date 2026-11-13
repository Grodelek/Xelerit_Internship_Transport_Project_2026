package com.internship.task.ride_together.controller;

import com.internship.task.ride_together.dto.JoinOfficeRequest;
import com.internship.task.ride_together.dto.OfficeRequest;
import com.internship.task.ride_together.dto.OfficeResponse;
import com.internship.task.ride_together.service.OfficeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offices")
public class OfficeController {

    private final OfficeService officeService;

    public OfficeController(OfficeService officeService) {
        this.officeService = officeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfficeResponse create(@Valid @RequestBody OfficeRequest request) {
        return officeService.createOffice(request);
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@Valid @RequestBody JoinOfficeRequest joinOfficeRequest, Authentication authentication) {
        officeService.joinOffice(joinOfficeRequest.organisationCode(), authentication.getName());
    }

    @GetMapping("/me")
    public OfficeResponse me(Authentication authentication) {
        return officeService.getOfficeOfUser(authentication.getName());
    }
}
