package com.simpleodonto.pago.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Wrapper sobre la API REST de MercadoPago. El acceso autenticado va con Bearer del access token
 * (env var MP_ACCESS_TOKEN). El webhook de MP solo manda IDs por seguridad — el cuerpo real del
 * pago / suscripción se consulta acá.
 */
@Service
@Slf4j
public class MercadoPagoService {

    private static final String MP_BASE = "https://api.mercadopago.com";

    private final RestClient http;
    private final String     accessToken;

    public MercadoPagoService(@Value("${app.mp.access-token}") String accessToken) {
        this.accessToken = accessToken;
        this.http        = RestClient.create();
    }

    /** GET /v1/payments/{id} — datos completos de un pago. */
    public Optional<JsonNode> obtenerPayment(String paymentId) {
        return get("/v1/payments/" + paymentId);
    }

    /** GET /preapproval/{id} — datos completos de una suscripción del usuario. */
    public Optional<JsonNode> obtenerPreapproval(String preapprovalId) {
        return get("/preapproval/" + preapprovalId);
    }

    /**
     * GET /authorized_payments/{id} — datos del cobro recurrente individual de una suscripción.
     * Llega para el evento {@code subscription_authorized_payment} cada vez que MP cobra (mensual).
     */
    public Optional<JsonNode> obtenerAuthorizedPayment(String authorizedPaymentId) {
        return get("/authorized_payments/" + authorizedPaymentId);
    }

    private Optional<JsonNode> get(String path) {
        try {
            JsonNode body = http.get()
                    .uri(MP_BASE + path)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.ofNullable(body);
        } catch (Exception e) {
            log.warn("[MP] Error consultando {}: {}", path, e.getMessage());
            return Optional.empty();
        }
    }
}
