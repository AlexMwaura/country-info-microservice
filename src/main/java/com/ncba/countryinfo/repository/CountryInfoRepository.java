package com.ncba.countryinfo.repository;

import com.ncba.countryinfo.entity.CountryInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryInfoRepository extends JpaRepository<CountryInfo, Long> {

    Optional<CountryInfo> findByCountryNameIgnoreCase(String countryName);

    Optional<CountryInfo> findByIsoCodeIgnoreCase(String isoCode);

    boolean existsByCountryNameIgnoreCase(String countryName);

    @Query("SELECT c FROM CountryInfo c LEFT JOIN FETCH c.languages")
    List<CountryInfo> findAllWithLanguages();

    @Query("SELECT c FROM CountryInfo c LEFT JOIN FETCH c.languages WHERE c.id = :id")
    Optional<CountryInfo> findByIdWithLanguages(Long id);
}
