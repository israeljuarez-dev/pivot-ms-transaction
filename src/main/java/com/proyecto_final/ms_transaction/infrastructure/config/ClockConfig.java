package com.proyecto_final.ms_transaction.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Expone el reloj como bean (para fijarlo en los tests), con la zona horaria del negocio.
 */
@Configuration
public class ClockConfig {

    /**
     * Reloj del sistema en la zona horaria del negocio. Determina qué es "hoy" y "este mes".
     *
     * @param zoneId zona horaria configurada en app.zone-id
     * @return reloj del sistema en esa zona
     */
    @Bean
    public Clock clock(@Value("${app.zone-id}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
