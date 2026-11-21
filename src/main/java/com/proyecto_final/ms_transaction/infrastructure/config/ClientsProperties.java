package com.proyecto_final.ms_transaction.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de los clientes HTTP hacia ms-account y ms-credit (prefijo clients).
 *
 * @param account configuración del cliente de ms-account
 * @param credit  configuración del cliente de ms-credit
 */
@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(ClientProperties account, ClientProperties credit) {

    /**
     * URL base de un servicio, con su nombre en Eureka.
     *
     * @param baseUrl URL base del servicio
     */
    public record ClientProperties(String baseUrl) {
    }
}
