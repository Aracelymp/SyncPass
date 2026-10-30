
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class CognitoTokenValidator {

    private final String issuer;
    private final String clientId;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public CognitoTokenValidator(
            @Value("${app.cognito.issuer}") String issuer,
            @Value("${app.cognito.client-id}") String clientId,
            @Value("${app.cognito.jwks-uri}") String jwksUri
    ) {
        this.issuer = issuer;
        this.clientId = clientId;

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

            // 1. Validar ISSUER
            String iss = claims.getIssuer();

            if (iss == null || !iss.equals(issuer)) {
                System.err.println(
                        "FALLO ISSUER: recibido=" + iss
                                + " esperado=" + issuer
                );

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token Cognito inválido"
                );
            }

            // 2. Validar expiración
            Date exp = claims.getExpirationTime();

            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                System.err.println("FALLO EXPIRATION: token expirado o sin fecha");

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token Cognito expirado"
                );
            }

            // 3. Validar tipo de token
            String tokenUse = claims.getStringClaim("token_use");

            if (tokenUse == null ||
                    (!tokenUse.equals("id") && !tokenUse.equals("access"))) {

                System.err.println("FALLO TOKEN_USE: " + tokenUse);

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token Cognito inválido"
                );
            }

            // 4. Validar CLIENT ID o AUDIENCE
            String clientIdClaim = claims.getStringClaim("client_id");

            if (clientIdClaim == null) {

                List<String> aud = claims.getAudience();

                if (aud == null || aud.isEmpty() || !aud.contains(clientId)) {
                    System.err.println(
                            "FALLO AUDIENCE: recibido=" + aud
                                    + " esperado=" + clientId
                    );

                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Token Cognito inválido"
                    );
                }

            } else if (!clientIdClaim.equals(clientId)) {

                System.err.println(
                        "FALLO CLIENT_ID: recibido=" + clientIdClaim
                                + " esperado=" + clientId
                );

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token Cognito inválido"
                );
            }

            // 5. Validar identificador del usuario
            String sub = claims.getSubject();

            if (sub == null || sub.isBlank()) {
                System.err.println("FALLO SUB: identificador de usuario ausente");

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Token Cognito inválido"
                );
            }

            System.out.println(
                    "COGNITO VALIDATION SUCCESS: usuario validado correctamente"
            );

            return claims;

        } catch (ResponseStatusException ex) {

            System.err.println(
                    "COGNITO VALIDATION: " + ex.getReason()
            );

            throw ex;

        } catch (Exception ex) {

            System.err.println(
                    "COGNITO EXCEPTION: "
                            + ex.getClass().getName()
                            + " - " + ex.getMessage()
            );

            ex.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token Cognito inválido"
            );
        }
    }
}
