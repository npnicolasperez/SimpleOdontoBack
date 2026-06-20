package com.simpleodonto.pago.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orquesta la creación de suscripciones (preapprovals) atadas al plan activo según MP_MODE.
 * AuthService la usa cuando un profesional se pre-registra para generarle un link de pago único.
 *
 * Separa la lógica de "qué modo / qué plan" de la lógica REST cruda (MercadoPagoService es agnóstica
 * al modo, solo recibe parámetros — este service decide qué mode y planId pasarle).
 */
@Service
@Slf4j
public class SuscripcionService {

    private final MercadoPagoService mp;
    private final String             mode;
    private final String             planIdTest;
    private final String             planIdProd;

    public SuscripcionService(MercadoPagoService mp,
                              @Value("${app.mp.mode}")                     String mode,
                              @Value("${app.mp.preapproval-plan-id-test}") String planIdTest,
                              @Value("${app.mp.preapproval-plan-id-prod}") String planIdProd) {
        this.mp         = mp;
        this.mode       = mode;
        this.planIdTest = planIdTest;
        this.planIdProd = planIdProd;
    }

    /**
     * Crea la preapproval del profesional. Devuelve un PreapprovalCreada con el id y el init_point
     * (URL que el profesional abre para completar el pago). Empty si la creación falla — el caller
     * decide qué hacer (típicamente continúa el registro y avisa al admin que MP falló).
     */
    public Optional<PreapprovalCreada> crearParaProfesional(String emailProfesional) {
        String planId = planIdActivo();
        if (planId == null || planId.isBlank()) {
            log.warn("[MP/{}] preapproval_plan_id vacío — saltando creación de preapproval para {}", mode, emailProfesional);
            return Optional.empty();
        }
        return mp.crearPreapproval(mode, planId, emailProfesional, emailProfesional)
                .map(node -> {
                    String id        = textOrNull(node, "id");
                    String initPoint = textOrNull(node, "init_point");
                    log.info("[MP/{}] preapproval creada para {}: id={}", mode, emailProfesional, id);
                    return new PreapprovalCreada(id, initPoint);
                });
    }

    private String planIdActivo() {
        return "prod".equalsIgnoreCase(mode) ? planIdProd : planIdTest;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    public record PreapprovalCreada(String id, String initPoint) {}
}
