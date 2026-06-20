package com.simpleodonto.pago.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Wrapper sobre la API REST de MercadoPago. Tiene ambos access tokens (test y prod) y elige cuál
 * usar por cada llamada según el {@code mode} recibido — el modo no vive en este servicio, lo
 * decide el caller (controller del webhook que recibe en /test o /prod).
 *
 * Los métodos son agnósticos al modo en su firma: reciben el ID a consultar + el modo.
 */
@Service
@Slf4j
public class MercadoPagoService {

    private static final String MP_BASE = "https://api.mercadopago.com";

    private final RestClient http;
    private final String     tokenTest;
    private final String     tokenProd;

    public MercadoPagoService(
            @Value("${app.mp.access-token-test}") String tokenTest,
            @Value("${app.mp.access-token-prod}") String tokenProd) {
        this.tokenTest = tokenTest;
        this.tokenProd = tokenProd;
        this.http      = RestClient.create();
    }

    /** GET /v1/payments/{id} — datos completos de un pago. */
    public Optional<JsonNode> obtenerPayment(String mode, String paymentId) {
        return get(mode, "/v1/payments/" + paymentId);
    }

    /** GET /preapproval/{id} — datos completos de una suscripción del usuario. */
    public Optional<JsonNode> obtenerPreapproval(String mode, String preapprovalId) {
        return get(mode, "/preapproval/" + preapprovalId);
    }

    /**
     * GET /authorized_payments/{id} — datos del cobro recurrente individual de una suscripción.
     * Llega para el evento {@code subscription_authorized_payment} cada vez que MP cobra (mensual).
     */
    public Optional<JsonNode> obtenerAuthorizedPayment(String mode, String authorizedPaymentId) {
        return get(mode, "/authorized_payments/" + authorizedPaymentId);
    }

    private Optional<JsonNode> get(String mode, String path) {
        String token = tokenFor(mode);
        if (token == null || token.isBlank()) {
            log.warn("[MP/{}] Access token vacío — no se puede consultar {}", mode, path);
            return Optional.empty();
        }
        try {
            JsonNode body = http.get()
                    .uri(MP_BASE + path)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.ofNullable(body);
        } catch (Exception e) {
            log.warn("[MP/{}] Error consultando {}: {}", mode, path, e.getMessage());
            return Optional.empty();
        }
    }

    private String tokenFor(String mode) {
        return "prod".equalsIgnoreCase(mode) ? tokenProd : tokenTest;
    }
}
