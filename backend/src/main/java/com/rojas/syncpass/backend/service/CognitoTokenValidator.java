
package com.rojas.syncpass.backend.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CognitoTokenValidator {

    private static final Logger logger =
            LoggerFactory.getLogger(CognitoTokenValidator.class);

    private static final long CLOCK_SKEW_SECONDS = 60;

    private final String issuer;
    private final Set<String> clientIds;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public CognitoTokenValidator(
            @Value("${app.cognito.issuer}") String issuer,
            @Value("${app.cognito.client-ids}") String clientIdsValue,
            @Value("${app.cognito.jwks-uri}") String jwksUri
    ) {
        this.issuer = issuer;

        this.clientIds = Arrays.stream(clientIdsValue.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        if (this.clientIds.isEmpty()) {
            throw new IllegalStateException(
                    "Debe configurarse al menos un App Client ID de Cognito"
            );
        }

        try {
            DefaultResourceRetriever retriever =
                    new DefaultResourceRetriever(2000, 2000);

            URL jwksUrl = new URL(jwksUri);

            JWKSource<SecurityContext> keySource =
                    new RemoteJWKSet<>(jwksUrl, retriever);

            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(
                            JWSAlgorithm.RS256,
                            keySource
                    );

            DefaultJWTProcessor<SecurityContext> processor =
                    new DefaultJWTProcessor<>();

            processor.setJWSKeySelector(keySelector);

            this.jwtProcessor = processor;

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "No se pudo inicializar la validación de tokens Cognito",
                    ex
            );
        }
    }

    public JWTClaimsSet validate(String token) {

        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token Cognito requerido"
            );
        }

        try {
            JWTClaimsSet claims = jwtProcessor.process(token, null);

            Instant now = Instant.now();

            // 1. Validar ISSUER
            String iss = claims.getIssuer();

            if (iss == null || !iss.equals(issuer)) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Emisor del token Cognito no autorizado"
                );
            }

            // 2. Validar EXPIRACIÓN
            Date exp = claims.getExpirationTime();

            if (exp == null || !exp.toInstant().isAfter(now)) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token Cognito expirado"
                );
            }

            // 3. Validar FECHA DE EMISIÓN
            Date iat = claims.getIssueTime();

            if (iat == null ||
                    iat.toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Fecha de emisión del token Cognito inválida"
                );
            }

            // 4. Validar FECHA DE INICIO, si está presente
            Date nbf = claims.getNotBeforeTime();

            if (nbf != null &&
                    nbf.toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "El token Cognito todavía no es válido"
                );
            }

            // 5. Validar TIPO DE TOKEN
            String tokenUse = claims.getStringClaim("token_use");

            if (tokenUse == null ||
                    (!tokenUse.equals("id") && !tokenUse.equals("access"))) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Tipo de token Cognito inválido"
                );
            }

            // 6. Validar CLIENTE según el tipo de token
            if ("id".equals(tokenUse)) {

                // Los ID Tokens identifican al cliente mediante aud.
                if (claims.getAudience() == null ||
                        claims.getAudience().stream()
                                .noneMatch(clientIds::contains)) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Audience del token Cognito no autorizado"
                    );
                }

            } else {

                // Los Access Tokens identifican al cliente mediante client_id.
                String clientIdClaim = claims.getStringClaim("client_id");

                if (clientIdClaim == null ||
                        !clientIds.contains(clientIdClaim)) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Client ID del token Cognito no autorizado"
                    );
                }
            }

            // 7. Validar IDENTIFICADOR DEL USUARIO
            String sub = claims.getSubject();

            if (sub == null || sub.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Identificador de usuario Cognito ausente"
                );
            }

            logger.info("Token Cognito validado correctamente");

            return claims;

        } catch (ResponseStatusException ex) {

            logger.warn("Validación Cognito rechazada: {}", ex.getReason());

            throw ex;

        } catch (Exception ex) {

            logger.error(
                    "Error al validar el token Cognito: {}",
                    ex.getMessage(),
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token Cognito inválido"
            );
        }
    }
}
