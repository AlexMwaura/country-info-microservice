package com.ncba.countryinfo.exception;

public class DuplicateCountryException extends RuntimeException {

    public DuplicateCountryException(String countryName) {
        super("Country already exists: " + countryName);
    }
}
