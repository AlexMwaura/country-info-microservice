package com.ncba.countryinfo.mapper;

import com.ncba.countryinfo.dto.CountryResponse;
import com.ncba.countryinfo.dto.LanguageDto;
import com.ncba.countryinfo.entity.CountryInfo;
import com.ncba.countryinfo.entity.Language;
import com.ncba.countryinfo.integration.generated.TCountryInfo;
import com.ncba.countryinfo.integration.generated.TLanguage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Maps between SOAP response types, JPA entities, and API DTOs.
 * Centralizes all data transformation logic to maintain single responsibility
 * and prevent mapping duplication across the service layer.
 */
@Component
public class CountryInfoMapper {

    public CountryInfo toEntity(TCountryInfo soapResponse, String isoCode) {
        CountryInfo country = CountryInfo.builder()
                .countryName(soapResponse.getSName())
                .isoCode(isoCode)
                .capitalCity(soapResponse.getSCapitalCity())
                .phoneCode(soapResponse.getSPhoneCode())
                .currencyISOCode(soapResponse.getSCurrencyISOCode())
                .countryFlagUrl(soapResponse.getSCountryFlag())
                .continentCode(soapResponse.getSContinentCode())
                .build();

        if (soapResponse.getLanguages() != null && soapResponse.getLanguages().getTLanguage() != null) {
            for (TLanguage tLang : soapResponse.getLanguages().getTLanguage()) {
                Language language = Language.builder()
                        .languageName(tLang.getSName())
                        .languageCode(tLang.getSISOCode())
                        .build();
                country.addLanguage(language);
            }
        }

        return country;
    }

    public CountryResponse toResponse(CountryInfo entity) {
        return CountryResponse.builder()
                .id(entity.getId())
                .countryName(entity.getCountryName())
                .isoCode(entity.getIsoCode())
                .capitalCity(entity.getCapitalCity())
                .phoneCode(entity.getPhoneCode())
                .currencyISOCode(entity.getCurrencyISOCode())
                .countryFlagUrl(entity.getCountryFlagUrl())
                .continentCode(entity.getContinentCode())
                .languages(toLanguageDtos(entity.getLanguages()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<CountryResponse> toResponseList(List<CountryInfo> entities) {
        return entities.stream().map(this::toResponse).toList();
    }

    private List<LanguageDto> toLanguageDtos(List<Language> languages) {
        if (languages == null) {
            return Collections.emptyList();
        }
        return languages.stream()
                .map(lang -> LanguageDto.builder()
                        .id(lang.getId())
                        .languageName(lang.getLanguageName())
                        .languageCode(lang.getLanguageCode())
                        .build())
                .toList();
    }
}
