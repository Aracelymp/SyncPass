package com.rojas.syncpass.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rojas.syncpass.backend.config.CustomUserDetailsService;
import com.rojas.syncpass.backend.config.JwtService;
import com.rojas.syncpass.backend.domain.Role;
import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.domain.UserCognitoLink;
import com.rojas.syncpass.backend.repo.UserCognitoLinkRepository;
import com.rojas.syncpass.backend.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CognitoExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCognitoLinkRepository userCognitoLinkRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rejectsInvalidToken() throws Exception {
        Map<String, String> body = Map.of("idToken", "not-a-jwt");
        mockMvc.perform(post("/api/auth/cognito/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnlinkedIdentity() throws Exception {
        String sub = UUID.randomUUID().toString();
        userCognitoLinkRepository.findByCognitoSub(sub).ifPresent(userCognitoLinkRepository::delete);
        Map<String, String> body = Map.of("idToken", buildFakeToken(sub, "id"));
        mockMvc.perform(post("/api/auth/cognito/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    private String buildFakeToken(String sub, String tokenUse) {
        String header = "eyJhbGciOiJSUzI1NiIsImtpZCI6InRlc3QifQ";
        String payloadJson = String.format("{\"iss\":\"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_eyoBw6XGI\",\"sub\":\"%s\",\"token_use\":\"%s\",\"client_id\":\"36tb2beisuo9gd5o3bekt3e7ac\",\"exp\":%d}", sub, tokenUse, System.currentTimeMillis() / 1000 + 3600);
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".sig";
    }
}
