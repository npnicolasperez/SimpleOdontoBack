package com.simpleodonto.shared.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Verifica tokens de Cloudflare Turnstile contra el endpoint oficial de siteverify.
 * El secret se inyecta vía env var TURNSTILE_SECRET (sin fallback — la app no arranca si falta).
 */
@Service
@Slf4j
public class TurnstileService {

    private static final String VERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final String     secret;
    private final RestClient http;

    public TurnstileService(@Value("${app.turnstile.secret}") String secret) {
        this.secret = secret;
        this.http   = RestClient.create();
    }

    public boolean verify(String token) {
        if (token == null || token.isBlank()) return false;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret",   secret);
        form.add("response", token);

        try {
            Response r = http.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Response.class);
            if (r == null) return false;
            if (!r.success()) {
                log.warn("Turnstile verify falló. Códigos: {}", r.errorCodes());
            }
            return r.success();
        } catch (Exception e) {
            log.warn("Error llamando a Turnstile siteverify: {}", e.getMessage());
            return false;
        }
    }

    private record Response(
            boolean success,
            @JsonProperty("error-codes") List<String> errorCodes
    ) {}
}
