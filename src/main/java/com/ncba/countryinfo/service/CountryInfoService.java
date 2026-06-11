package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryResponse;
import com.ncba.countryinfo.dto.CountryUpdateRequest;
import com.ncba.countryinfo.entity.CountryInfo;
import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.DuplicateCountryException;
import com.ncba.countryinfo.integration.CountryInfoSoapClient;
import com.ncba.countryinfo.integration.generated.TCountryInfo;
import com.ncba.countryinfo.mapper.CountryInfoMapper;
import com.ncba.countryinfo.repository.CountryInfoRepository;
import com.ncba.countryinfo.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core service orchestrating the country information retrieval workflow.
 * Coordinates between the SOAP integration layer and the persistence layer,
 * applying business rules such as name normalization and duplicate detection.
 */
@Service
public class CountryInfoService {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoService.class);

    private final CountryInfoSoapClient soapClient;
    private final CountryInfoRepository repository;
    private final CountryInfoMapper mapper;

    public CountryInfoService(CountryInfoSoapClient soapClient,
                              CountryInfoRepository repository,
                              CountryInfoMapper mapper) {
        this.soapClient = soapClient;
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Orchestrates the full country lookup workflow:
     * 1. Normalizes the country name to sentence case
     * 2. Checks for existing persisted record (idempotent behavior)
     * 3. Calls SOAP to resolve country name -> ISO code
     * 4. Calls SOAP to fetch full country info using the ISO code
     * 5. Maps and persists the aggregated country profile
     */
    @Transactional
    public CountryResponse lookupAndPersistCountry(String rawCountryName) {
        String normalizedName = StringUtils.toSentenceCase(rawCountryName);
        log.info("Country lookup initiated for country={}", normalizedName);

        var existing = repository.findByCountryNameIgnoreCase(normalizedName);
        if (existing.isPresent()) {
            log.info("Country={} already persisted, returning existing record id={}", normalizedName, existing.get().getId());
            throw new DuplicateCountryException(normalizedName);
        }

        String isoCode = soapClient.getCountryIsoCode(normalizedName);
        log.info("ISO code resolved: country={}, isoCode={}", normalizedName, isoCode);

        TCountryInfo soapCountryInfo = soapClient.getFullCountryInfo(isoCode);
        log.info("Full country info retrieved for isoCode={}", isoCode);

        CountryInfo entity = mapper.toEntity(soapCountryInfo, isoCode);
        CountryInfo persisted = repository.save(entity);
        log.info("Country profile persisted: id={}, country={}, isoCode={}", persisted.getId(), normalizedName, isoCode);

        return mapper.toResponse(persisted);
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> getAllCountries() {
        log.info("Fetching all persisted country records");
        List<CountryInfo> countries = repository.findAllWithLanguages();
        log.info("Retrieved {} country records from database", countries.size());
        return mapper.toResponseList(countries);
    }

    @Transactional(readOnly = true)
    public CountryResponse getCountryById(Long id) {
        log.info("Fetching country record by id={}", id);
        CountryInfo country = repository.findByIdWithLanguages(id)
                .orElseThrow(() -> new CountryNotFoundException(id));
        return mapper.toResponse(country);
    }

    @Transactional
    public CountryResponse updateCountry(Long id, CountryUpdateRequest request) {
        log.info("Updating country record id={}", id);
        CountryInfo country = repository.findByIdWithLanguages(id)
                .orElseThrow(() -> new CountryNotFoundException(id));

        if (request.getCountryName() != null) {
            country.setCountryName(StringUtils.toSentenceCase(request.getCountryName()));
        }
        if (request.getCapitalCity() != null) {
            country.setCapitalCity(request.getCapitalCity());
        }
        if (request.getPhoneCode() != null) {
            country.setPhoneCode(request.getPhoneCode());
        }
        if (request.getCurrencyISOCode() != null) {
            country.setCurrencyISOCode(request.getCurrencyISOCode());
        }

        CountryInfo updated = repository.save(country);
        log.info("Country record updated: id={}, country={}", updated.getId(), updated.getCountryName());
        return mapper.toResponse(updated);
    }

    @Transactional
    public void deleteCountry(Long id) {
        log.info("Deleting country record id={}", id);
        if (!repository.existsById(id)) {
            throw new CountryNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Country record deleted: id={}", id);
    }
}
