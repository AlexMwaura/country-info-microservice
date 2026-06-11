package com.ncba.countryinfo.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryUpdateRequest {

    @Size(min = 2, max = 100, message = "Country name must be between 2 and 100 characters")
    private String countryName;

    @Size(max = 100, message = "Capital city must not exceed 100 characters")
    private String capitalCity;

    @Size(max = 20, message = "Phone code must not exceed 20 characters")
    private String phoneCode;

    @Size(max = 10, message = "Currency ISO code must not exceed 10 characters")
    private String currencyISOCode;
}
