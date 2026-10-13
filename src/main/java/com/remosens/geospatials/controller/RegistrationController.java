package com.remosens.geospatials.controller;

import com.remosens.geospatials.dto.ExpertRegistrationRequest;
import com.remosens.geospatials.dto.LearnerRegistrationRequest;
import com.remosens.geospatials.dto.PartnerRegistrationRequest;
import com.remosens.geospatials.dto.RegistrationResponse;
import com.remosens.geospatials.service.ExpertRegistrationService;
import com.remosens.geospatials.service.LearnerRegistrationService;
import com.remosens.geospatials.service.PartnerRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final LearnerRegistrationService learnerService;
    private final ExpertRegistrationService expertService;
    private final PartnerRegistrationService partnerService;

    @PostMapping("/learners")
    public ResponseEntity<RegistrationResponse> registerLearner(
            @Valid @RequestBody LearnerRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(learnerService.register(request));
    }

    @PostMapping("/experts")
    public ResponseEntity<RegistrationResponse> registerExpert(
            @Valid @RequestBody ExpertRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expertService.register(request));
    }

    @PostMapping("/partners")
    public ResponseEntity<RegistrationResponse> registerPartner(
            @Valid @RequestBody PartnerRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.register(request));
    }
}
