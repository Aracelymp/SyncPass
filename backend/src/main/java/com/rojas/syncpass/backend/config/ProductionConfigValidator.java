package com.rojas.syncpass.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validaciones de configuracion obligatorias para el perfil de produccion.
 * Este componente solo se activa con la profile "prod".
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionConfigValidator implements CommandLineRunner {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    public void run(String... args) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "La variable de entorno JWT_SECRET es obligatoria en produccion."
            );
        }
        if (jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes (256 bits). " +
                    "Se recomiendan 64 caracteres aleatorios."
            );
        }
    }
}
