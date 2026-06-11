package com.ncba.countryinfo.integration;

import com.ncba.countryinfo.integration.generated.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * SOAP client that orchestrates calls to the CountryInfoService WSDL endpoint.
 * Wraps generated JAX-WS stubs with resilience patterns (circuit breaker, retry)
 * and caching to protect the service from upstream failures.
 */
@Component
public class CountryInfoSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoSoapClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "soapService";
    private static final String RETRY_NAME = "soapService";

    private final CountryInfoServiceSoapType soapPort;

    public CountryInfoSoapClient(CountryInfoServiceSoapType soapPort) {
        this.soapPort = soapPort;
    }

    /**
     * Resolves a country name to its ISO code via the CountryISOCode SOAP operation.
     *
     * @param countryName normalized country name (sentence case)
     * @return ISO country code (e.g., "KE" for Kenya)
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getCountryIsoCodeFallback")
    @Retry(name = RETRY_NAME)
    @Cacheable(value = "isoCodeCache", key = "#countryName")
    public String getCountryIsoCode(String countryName) {
        log.info("Resolving ISO code for country={} via SOAP", countryName);
        String isoCode = soapPort.countryISOCode(countryName);

        if (isoCode == null || isoCode.isBlank() || "No country found by that name".equalsIgnoreCase(isoCode)) {
            log.warn("SOAP service returned no valid ISO code for country={}, response={}", countryName, isoCode);
            throw new CountrySoapLookupException("No ISO code found for country: " + countryName);
        }

        log.info("Resolved country={} to isoCode={}", countryName, isoCode);
        return isoCode;
    }

    /**
     * Fetches the full country information using the ISO code via the FullCountryInfo SOAP operation.
     *
     * @param isoCode ISO country code
     * @return full country info response object from the SOAP service
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getFullCountryInfoFallback")
    @Retry(name = RETRY_NAME)
    @Cacheable(value = "countryInfoCache", key = "#isoCode")
    public TCountryInfo getFullCountryInfo(String isoCode) {
        log.info("Fetching full country info for isoCode={} via SOAP", isoCode);
        TCountryInfo countryInfo = soapPort.fullCountryInfo(isoCode);

        if (countryInfo == null || countryInfo.getSISOCode() == null || countryInfo.getSISOCode().isBlank()) {
            log.warn("SOAP service returned empty country info for isoCode={}", isoCode);
            throw new CountrySoapLookupException("No country info found for ISO code: " + isoCode);
        }

        log.info("Retrieved full country info for isoCode={}, country={}", isoCode, countryInfo.getSName());
        return countryInfo;
    }

    @SuppressWarnings("unused")
    private String getCountryIsoCodeFallback(String countryName, Throwable t) {
        log.error("Circuit breaker open for ISO code lookup, country={}, cause={}", countryName, t.getMessage());
        throw new CountrySoapLookupException("Country info service is temporarily unavailable. Please try again later.", t);
    }

    @SuppressWarnings("unused")
    private TCountryInfo getFullCountryInfoFallback(String isoCode, Throwable t) {
        log.error("Circuit breaker open for full country info lookup, isoCode={}, cause={}", isoCode, t.getMessage());
        throw new CountrySoapLookupException("Country info service is temporarily unavailable. Please try again later.", t);
    }
}
