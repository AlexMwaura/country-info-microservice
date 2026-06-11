package com.ncba.countryinfo.controller;

import com.ncba.countryinfo.dto.CountryRequest;
import com.ncba.countryinfo.dto.CountryResponse;
import com.ncba.countryinfo.dto.CountryUpdateRequest;
import com.ncba.countryinfo.service.CountryInfoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryInfoController {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoController.class);

    private final CountryInfoService countryInfoService;

    public CountryInfoController(CountryInfoService countryInfoService) {
        this.countryInfoService = countryInfoService;
    }

    @PostMapping
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) {
        log.info("POST /api/v1/countries - country lookup requested for name={}", request.getName());
        CountryResponse response = countryInfoService.lookupAndPersistCountry(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> getAllCountries() {
        log.info("GET /api/v1/countries - fetching all countries");
        return ResponseEntity.ok(countryInfoService.getAllCountries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryResponse> getCountryById(@PathVariable Long id) {
        log.info("GET /api/v1/countries/{} - fetching country by id", id);
        return ResponseEntity.ok(countryInfoService.getCountryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryResponse> updateCountry(@PathVariable Long id,
                                                          @Valid @RequestBody CountryUpdateRequest request) {
        log.info("PUT /api/v1/countries/{} - updating country", id);
        return ResponseEntity.ok(countryInfoService.updateCountry(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountry(@PathVariable Long id) {
        log.info("DELETE /api/v1/countries/{} - deleting country", id);
        countryInfoService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }
}
