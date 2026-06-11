package com.ncba.countryinfo.config;

import com.ncba.countryinfo.integration.generated.CountryInfoService;
import com.ncba.countryinfo.integration.generated.CountryInfoServiceSoapType;
import jakarta.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the JAX-WS SOAP client with production-grade timeout settings.
 * The generated CountryInfoService stub is instantiated once and reused
 * across all requests as JAX-WS ports are thread-safe for read operations.
 */
@Configuration
public class SoapClientConfig {

    private static final Logger log = LoggerFactory.getLogger(SoapClientConfig.class);

    @Value("${soap.client.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${soap.client.read-timeout:10000}")
    private int readTimeout;

    @Value("${soap.client.endpoint-url:http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso}")
    private String endpointUrl;

    @Bean
    public CountryInfoServiceSoapType countryInfoServiceSoapPort() {
        log.info("Initializing SOAP client with endpoint={}, connectTimeout={}ms, readTimeout={}ms",
                endpointUrl, connectTimeout, readTimeout);

        CountryInfoService service = new CountryInfoService();
        CountryInfoServiceSoapType port = service.getCountryInfoServiceSoap();

        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
        bindingProvider.getRequestContext().put("com.sun.xml.ws.connect.timeout", connectTimeout);
        bindingProvider.getRequestContext().put("com.sun.xml.ws.request.timeout", readTimeout);

        return port;
    }
}
