package com.ncba.countryinfo.integration;

/**
 * Thrown when the external CountryInfo SOAP service returns an error,
 * an unresolvable country, or is unreachable.
 */
public class CountrySoapLookupException extends RuntimeException {

    public CountrySoapLookupException(String message) {
        super(message);
    }

    public CountrySoapLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
