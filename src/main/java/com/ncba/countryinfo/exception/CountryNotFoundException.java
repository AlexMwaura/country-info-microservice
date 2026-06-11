package com.ncba.countryinfo.exception;

public class CountryNotFoundException extends RuntimeException {

    public CountryNotFoundException(Long id) {
        super("Country not found with id: " + id);
    }

    public CountryNotFoundException(String message) {
        super(message);
    }
}
