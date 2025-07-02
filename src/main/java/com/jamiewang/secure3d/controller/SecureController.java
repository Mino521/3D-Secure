package com.jamiewang.secure3d.controller;

import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.service.ILookUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST Controller for 3DS Card Range Lookup API
 *
 * Simplified API with only two essential endpoints:
 * - Card range lookup by PAN
 * - Bulk import of PRes messages
 */
@RestController
@RequestMapping("/api/v1/3d-secure")
@Validated
@Tag(name = "3D Secure API")
@Slf4j
public class SecureController {

    @Autowired
    private ILookUpService lookUpService;

    /**
     * Lookup card range by PAN
     */
    @GetMapping("/lookup")
    @Operation(
            summary = "Lookup 3DS Method URL and card range by PAN",
            description = "Find the matching card range and 3DS Method URL for a given Primary Account Number (PAN)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card range found"),
            @ApiResponse(responseCode = "404", description = "No card range found for the given PAN"),
            @ApiResponse(responseCode = "400", description = "Invalid PAN format")
    })
    public ResponseEntity<CardRangeDataDTO> lookupByPan(
            @Parameter(description = "Primary Account Number (PAN) - 16 digits", required = true)
            @RequestParam("pan")
            Long pan) {

        log.info("Received lookup request for PAN: {}", pan);

        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(pan);

        if (result.isPresent()) {
            log.info("Found card range for PAN: {}", pan);
            return ResponseEntity.ok(result.get());
        } else {
            log.info("No card range found for PAN: {}", pan);
            return ResponseEntity.notFound().build();
        }
    }
}
