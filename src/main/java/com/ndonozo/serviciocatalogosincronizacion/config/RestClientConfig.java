package com.ndonozo.serviciocatalogosincronizacion.config;
import com.ndonozo.serviciocatalogosincronizacion.config.CatedraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;


@Configuration
@EnableConfigurationProperties(CatedraProperties.class)
public class RestClientConfig {
    @Bean
    public RestClient catedraRestClient(RestClient.Builder builder, CatedraProperties properties){
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.jwt()).build();
    }
}