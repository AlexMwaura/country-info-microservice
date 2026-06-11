package com.ncba.countryinfo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryResponse {

    private Long id;
    private String countryName;
    private String isoCode;
    private String capitalCity;
    private String phoneCode;
    private String currencyISOCode;
    private String countryFlagUrl;
    private String continentCode;
    private List<LanguageDto> languages;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
