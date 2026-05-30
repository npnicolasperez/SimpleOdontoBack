package com.simpleodonto.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Wrapper sobre la API de Resend (https://resend.com/docs).
 * El api-key se inyecta vía env var RESEND_API_KEY (sin fallback — la app no arranca si falta).
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final String     apiKey;
    private final String     from;
    private final RestClient http;

    public EmailService(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.from:onboarding@resend.dev}") String from) {
        this.apiKey = apiKey;
        this.from   = from;
        this.http   = RestClient.create();
    }

    /**
     * Envía un email HTML. Devuelve true si Resend aceptó la petición.
     * Pensado para llamarse desde un método @Async — no relanza excepciones.
     */
    public boolean send(List<String> to, String subject, String html) {
        if (to == null || to.isEmpty()) {
            log.warn("EmailService.send invocado sin destinatarios");
            return false;
        }
        try {
            Map<String, Object> body = Map.of(
                    "from",    from,
                    "to",      to,
                    "subject", subject,
                    "html",    html
            );
            http.post()
                    .uri(RESEND_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Error enviando email vía Resend: {}", e.getMessage());
            return false;
        }
    }
}
