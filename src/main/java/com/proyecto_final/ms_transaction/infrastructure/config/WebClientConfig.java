package com.proyecto_final.ms_transaction.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configura los WebClient con los que ms-transaction consulta a ms-account y ms-credit.
 */
@Configuration
@EnableConfigurationProperties(ClientsProperties.class)
public class WebClientConfig {

    /**
     * Builder con balanceo de carga: reemplaza el nombre del servicio de la URL
     * por la dirección real que Eureka tiene registrada.
     *
     * @return builder de WebClient con balanceo
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * WebClient preconfigurado con la URL base de ms-account.
     */
    @Bean
    public WebClient accountWebClient(WebClient.Builder loadBalancedWebClientBuilder, ClientsProperties properties) {
        return buildWebClient(loadBalancedWebClientBuilder, properties.account().baseUrl());
    }

    /**
     * WebClient preconfigurado con la URL base de ms-credit.
     */
    @Bean
    public WebClient creditWebClient(WebClient.Builder loadBalancedWebClientBuilder, ClientsProperties properties) {
        return buildWebClient(loadBalancedWebClientBuilder, properties.credit().baseUrl());
    }

    // evita modificar el builder compartido entre los dos clientes
    private WebClient buildWebClient(WebClient.Builder loadBalancedWebClientBuilder, String baseUrl) {
        return loadBalancedWebClientBuilder.clone()
                .baseUrl(baseUrl)
                .build();
    }
}
