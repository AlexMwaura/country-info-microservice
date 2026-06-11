package com.ncba.countryinfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncba.countryinfo.dto.CountryRequest;
import com.ncba.countryinfo.dto.CountryResponse;
import com.ncba.countryinfo.dto.CountryUpdateRequest;
import com.ncba.countryinfo.dto.LanguageDto;
import com.ncba.countryinfo.exception.CountryNotFoundException;
import com.ncba.countryinfo.exception.GlobalExceptionHandler;
import com.ncba.countryinfo.service.CountryInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CountryInfoController.class)
@Import(GlobalExceptionHandler.class)
class CountryInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CountryInfoService countryInfoService;

    private CountryResponse buildSampleResponse() {
        return CountryResponse.builder()
                .id(1L)
                .countryName("Kenya")
                .isoCode("KE")
                .capitalCity("Nairobi")
                .phoneCode("254")
                .currencyISOCode("KES")
                .countryFlagUrl("http://example.com/flags/Kenya.png")
                .continentCode("AF")
                .languages(List.of(LanguageDto.builder()
                        .id(1L)
                        .languageName("Swahili")
                        .languageCode("swa")
                        .build()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/countries - should create and return country with 201")
    void createCountry_validRequest_returns201() throws Exception {
        when(countryInfoService.lookupAndPersistCountry("kenya")).thenReturn(buildSampleResponse());

        mockMvc.perform(post("/api/v1/countries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CountryRequest("kenya"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countryName").value("Kenya"))
                .andExpect(jsonPath("$.isoCode").value("KE"))
                .andExpect(jsonPath("$.capitalCity").value("Nairobi"))
                .andExpect(jsonPath("$.languages[0].languageName").value("Swahili"));
    }

    @Test
    @DisplayName("POST /api/v1/countries - should return 400 for blank name")
    void createCountry_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/countries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CountryRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/countries - should return all countries")
    void getAllCountries_returns200() throws Exception {
        when(countryInfoService.getAllCountries()).thenReturn(List.of(buildSampleResponse()));

        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryName").value("Kenya"));
    }

    @Test
    @DisplayName("GET /api/v1/countries/{id} - should return country by ID")
    void getCountryById_found_returns200() throws Exception {
        when(countryInfoService.getCountryById(1L)).thenReturn(buildSampleResponse());

        mockMvc.perform(get("/api/v1/countries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.countryName").value("Kenya"));
    }

    @Test
    @DisplayName("GET /api/v1/countries/{id} - should return 404 for non-existent ID")
    void getCountryById_notFound_returns404() throws Exception {
        when(countryInfoService.getCountryById(99L)).thenThrow(new CountryNotFoundException(99L));

        mockMvc.perform(get("/api/v1/countries/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("PUT /api/v1/countries/{id} - should update and return country")
    void updateCountry_validRequest_returns200() throws Exception {
        CountryResponse updated = buildSampleResponse();
        updated.setCapitalCity("Updated Capital");
        when(countryInfoService.updateCountry(eq(1L), any(CountryUpdateRequest.class))).thenReturn(updated);

        CountryUpdateRequest updateRequest = new CountryUpdateRequest();
        updateRequest.setCapitalCity("Updated Capital");

        mockMvc.perform(put("/api/v1/countries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capitalCity").value("Updated Capital"));
    }

    @Test
    @DisplayName("DELETE /api/v1/countries/{id} - should return 204 on successful deletion")
    void deleteCountry_exists_returns204() throws Exception {
        doNothing().when(countryInfoService).deleteCountry(1L);

        mockMvc.perform(delete("/api/v1/countries/1"))
                .andExpect(status().isNoContent());
    }
}
