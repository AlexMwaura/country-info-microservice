package com.ncba.countryinfo.service;

import com.ncba.countryinfo.util.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @ParameterizedTest
    @DisplayName("Should convert various country names to sentence case")
    @CsvSource({
            "kenya, Kenya",
            "KENYA, Kenya",
            "united states, United States",
            "UNITED KINGDOM, United Kingdom",
            "south africa, South Africa",
            "cote d'ivoire, Cote D'ivoire",
    })
    void toSentenceCase_variousInputs(String input, String expected) {
        assertThat(StringUtils.toSentenceCase(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle null and blank inputs gracefully")
    void toSentenceCase_nullAndBlank() {
        assertThat(StringUtils.toSentenceCase(null)).isNull();
        assertThat(StringUtils.toSentenceCase("  ")).isEqualTo("  ");
    }
}
