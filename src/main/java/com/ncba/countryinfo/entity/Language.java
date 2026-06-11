package com.ncba.countryinfo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "language", indexes = {
        @Index(name = "idx_language_country", columnList = "country_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String languageName;

    @Column(nullable = false, length = 10)
    private String languageCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryInfo countryInfo;
}
