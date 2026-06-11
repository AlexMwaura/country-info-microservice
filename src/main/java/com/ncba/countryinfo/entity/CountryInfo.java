package com.ncba.countryinfo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "country_info", indexes = {
        @Index(name = "idx_country_name", columnList = "countryName", unique = true),
        @Index(name = "idx_iso_code", columnList = "isoCode", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String countryName;

    @Column(nullable = false, unique = true, length = 10)
    private String isoCode;

    @Column(length = 100)
    private String capitalCity;

    @Column(length = 20)
    private String phoneCode;

    @Column(length = 10)
    private String currencyISOCode;

    @Column(length = 500)
    private String countryFlagUrl;

    @Column(length = 100)
    private String continentCode;

    @OneToMany(mappedBy = "countryInfo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Language> languages = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addLanguage(Language language) {
        languages.add(language);
        language.setCountryInfo(this);
    }

    public void clearLanguages() {
        languages.forEach(lang -> lang.setCountryInfo(null));
        languages.clear();
    }
}
