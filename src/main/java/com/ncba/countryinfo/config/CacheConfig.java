package com.ncba.countryinfo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine as the in-process cache for SOAP response data.
 * Country information is semi-static (capitals, ISO codes rarely change),
 * making it an ideal candidate for aggressive caching with a 24-hour TTL.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("isoCodeCache", "countryInfoCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }
}
