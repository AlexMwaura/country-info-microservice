package com.ncba.countryinfo.integration;

import com.ncba.countryinfo.integration.generated.ArrayOftLanguage;
import com.ncba.countryinfo.integration.generated.CountryInfoServiceSoapType;
import com.ncba.countryinfo.integration.generated.TCountryInfo;
import com.ncba.countryinfo.integration.generated.TLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryInfoSoapClientTest {

    @Mock
    private CountryInfoServiceSoapType soapPort;

    @InjectMocks
    private CountryInfoSoapClient soapClient;

    @Test
    @DisplayName("Should return ISO code for a valid country name")
    void getCountryIsoCode_validCountry_returnsCode() {
        when(soapPort.countryISOCode("Kenya")).thenReturn("KE");

        String result = soapClient.getCountryIsoCode("Kenya");

        assertThat(result).isEqualTo("KE");
        verify(soapPort).countryISOCode("Kenya");
    }

    @Test
    @DisplayName("Should throw when SOAP returns no valid ISO code")
    void getCountryIsoCode_invalidCountry_throws() {
        when(soapPort.countryISOCode("Narnia")).thenReturn("No country found by that name");

        assertThatThrownBy(() -> soapClient.getCountryIsoCode("Narnia"))
                .isInstanceOf(CountrySoapLookupException.class)
                .hasMessageContaining("No ISO code found");
    }

    @Test
    @DisplayName("Should return full country info for a valid ISO code")
    void getFullCountryInfo_validCode_returnsInfo() {
        TCountryInfo info = new TCountryInfo();
        info.setSISOCode("KE");
        info.setSName("Kenya");
        info.setSCapitalCity("Nairobi");
        TLanguage lang = new TLanguage();
        lang.setSName("Swahili");
        lang.setSISOCode("swa");
        ArrayOftLanguage langs = new ArrayOftLanguage();
        langs.getTLanguage().add(lang);
        info.setLanguages(langs);

        when(soapPort.fullCountryInfo("KE")).thenReturn(info);

        TCountryInfo result = soapClient.getFullCountryInfo("KE");

        assertThat(result.getSName()).isEqualTo("Kenya");
        assertThat(result.getSCapitalCity()).isEqualTo("Nairobi");
        assertThat(result.getLanguages().getTLanguage()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw when SOAP returns null country info")
    void getFullCountryInfo_nullResponse_throws() {
        when(soapPort.fullCountryInfo("XX")).thenReturn(null);

        assertThatThrownBy(() -> soapClient.getFullCountryInfo("XX"))
                .isInstanceOf(CountrySoapLookupException.class)
                .hasMessageContaining("No country info found");
    }
}
