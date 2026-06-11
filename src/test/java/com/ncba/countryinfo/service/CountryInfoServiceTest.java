package com.ncba.countryinfo.service;

import com.ncba.countryinfo.dto.CountryResponse;
import com.ncba.countryinfo.dto.CountryUpdateRequest;
import com.ncba.countryinfo.entity.CountryInfo;
import com.ncba.countryinfo.entity.Language;
import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.DuplicateCountryException;
import com.ncba.countryinfo.integration.CountryInfoSoapClient;
import com.ncba.countryinfo.integration.generated.ArrayOftLanguage;
import com.ncba.countryinfo.integration.generated.TCountryInfo;
import com.ncba.countryinfo.integration.generated.TLanguage;
import com.ncba.countryinfo.mapper.CountryInfoMapper;
import com.ncba.countryinfo.repository.CountryInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryInfoServiceTest {

    @Mock
    private CountryInfoSoapClient soapClient;

    @Mock
    private CountryInfoRepository repository;

    @Spy
    private CountryInfoMapper mapper;

    @InjectMocks
    private CountryInfoService service;

    private CountryInfo sampleEntity;
    private TCountryInfo sampleSoapResponse;

    @BeforeEach
    void setUp() {
        Language swahili = Language.builder()
                .id(1L)
                .languageName("Swahili")
                .languageCode("swa")
                .build();

        sampleEntity = CountryInfo.builder()
                .id(1L)
                .countryName("Kenya")
                .isoCode("KE")
                .capitalCity("Nairobi")
                .phoneCode("254")
                .currencyISOCode("KES")
                .countryFlagUrl("http://example.com/flags/Kenya.png")
                .continentCode("AF")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        sampleEntity.addLanguage(swahili);

        TLanguage tLang = new TLanguage();
        tLang.setSName("Swahili");
        tLang.setSISOCode("swa");

        ArrayOftLanguage languages = new ArrayOftLanguage();
        languages.getTLanguage().add(tLang);

        sampleSoapResponse = new TCountryInfo();
        sampleSoapResponse.setSName("Kenya");
        sampleSoapResponse.setSISOCode("KE");
        sampleSoapResponse.setSCapitalCity("Nairobi");
        sampleSoapResponse.setSPhoneCode("254");
        sampleSoapResponse.setSCurrencyISOCode("KES");
        sampleSoapResponse.setSCountryFlag("http://example.com/flags/Kenya.png");
        sampleSoapResponse.setSContinentCode("AF");
        sampleSoapResponse.setLanguages(languages);
    }

    @Test
    @DisplayName("Should lookup country via SOAP and persist when not already stored")
    void lookupAndPersistCountry_newCountry_persistsAndReturns() {
        when(repository.findByCountryNameIgnoreCase("Kenya")).thenReturn(Optional.empty());
        when(soapClient.getCountryIsoCode("Kenya")).thenReturn("KE");
        when(soapClient.getFullCountryInfo("KE")).thenReturn(sampleSoapResponse);
        when(repository.save(any(CountryInfo.class))).thenReturn(sampleEntity);

        CountryResponse response = service.lookupAndPersistCountry("kenya");

        assertThat(response.getCountryName()).isEqualTo("Kenya");
        assertThat(response.getIsoCode()).isEqualTo("KE");
        assertThat(response.getCapitalCity()).isEqualTo("Nairobi");
        assertThat(response.getLanguages()).hasSize(1);
        assertThat(response.getLanguages().get(0).getLanguageName()).isEqualTo("Swahili");

        verify(soapClient).getCountryIsoCode("Kenya");
        verify(soapClient).getFullCountryInfo("KE");
        verify(repository).save(any(CountryInfo.class));
    }

    @Test
    @DisplayName("Should throw DuplicateCountryException when country already exists")
    void lookupAndPersistCountry_existingCountry_throwsDuplicate() {
        when(repository.findByCountryNameIgnoreCase("Kenya")).thenReturn(Optional.of(sampleEntity));

        assertThatThrownBy(() -> service.lookupAndPersistCountry("kenya"))
                .isInstanceOf(DuplicateCountryException.class)
                .hasMessageContaining("Kenya");

        verifyNoInteractions(soapClient);
    }

    @Test
    @DisplayName("Should return all countries with languages eagerly fetched")
    void getAllCountries_returnsAll() {
        when(repository.findAllWithLanguages()).thenReturn(List.of(sampleEntity));

        List<CountryResponse> result = service.getAllCountries();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCountryName()).isEqualTo("Kenya");
    }

    @Test
    @DisplayName("Should return country by ID")
    void getCountryById_found_returnsCountry() {
        when(repository.findByIdWithLanguages(1L)).thenReturn(Optional.of(sampleEntity));

        CountryResponse result = service.getCountryById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCountryName()).isEqualTo("Kenya");
    }

    @Test
    @DisplayName("Should throw CountryNotFoundException for non-existent ID")
    void getCountryById_notFound_throws() {
        when(repository.findByIdWithLanguages(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCountryById(99L))
                .isInstanceOf(CountryNotFoundException.class);
    }

    @Test
    @DisplayName("Should update mutable fields of an existing country")
    void updateCountry_validUpdate_returnsUpdated() {
        when(repository.findByIdWithLanguages(1L)).thenReturn(Optional.of(sampleEntity));
        when(repository.save(any(CountryInfo.class))).thenReturn(sampleEntity);

        CountryUpdateRequest request = new CountryUpdateRequest();
        request.setCapitalCity("New Nairobi");

        CountryResponse result = service.updateCountry(1L, request);

        assertThat(result).isNotNull();
        verify(repository).save(any(CountryInfo.class));
    }

    @Test
    @DisplayName("Should delete country when it exists")
    void deleteCountry_exists_deletes() {
        when(repository.existsById(1L)).thenReturn(true);

        service.deleteCountry(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw CountryNotFoundException when deleting non-existent country")
    void deleteCountry_notFound_throws() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCountry(99L))
                .isInstanceOf(CountryNotFoundException.class);
    }
}
