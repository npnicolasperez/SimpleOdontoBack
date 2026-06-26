package com.simpleodonto.pago.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Construye el link de checkout de la suscripción (preapproval) para un profesional. Usa el plan
 * activo según MP_MODE y le inyecta el {@code external_reference} en la URL — cuando el
 * profesional paga, MP crea la preapproval con ese external_reference y el webhook llega con esa
 * data, lo que permite activar al profesional automáticamente.
 *
 * No creamos la preapproval desde el back porque MP exige {@code card_token_id} (tarjeta ya
 * tokenizada) y eso requiere que el user pase por el frontend de MP primero. Es más simple dejar
 * que MP la cree solo cuando el user paga desde el link.
 */
@Service
@Slf4j
public class SuscripcionService {

    private static final String CHECKOUT_URL = "https://www.mercadopago.com.ar/subscriptions/checkout";

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
     * Valida que la preapproval indicada esté autorizada en MP. Se usa desde el endpoint
     * confirmar-pago, al que el front llama cuando MP redirige al user a /post-pago con el
     * preapproval_id en el query string.
     */
    public boolean estaAutorizada(String preapprovalId) {
        return mp.obtenerPreapproval(mode, preapprovalId)
                .map(node -> {
                    String status = textOrNull(node, "status");
                    log.info("[MP/{}] Confirmar pago: preapproval {} status={}", mode, preapprovalId, status);
                    return "authorized".equals(status);
                })
                .orElse(false);
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /**
     * Devuelve el init_point (URL de checkout) para un profesional pre-registrado. Empty si el
     * plan no está configurado.
     */
    public Optional<String> initPointPara(String emailProfesional) {
        String planId = planIdActivo();
        if (planId == null || planId.isBlank()) {
            log.warn("[MP/{}] preapproval_plan_id vacío — no se puede generar link de pago para {}", mode, emailProfesional);
            return Optional.empty();
        }
        String url = CHECKOUT_URL
                + "?preapproval_plan_id=" + planId
                + "&external_reference="  + URLEncoder.encode(emailProfesional, StandardCharsets.UTF_8);
        log.info("[MP/{}] init_point generado para {}", mode, emailProfesional);
        return Optional.of(url);
    }

    private String planIdActivo() {
        return "prod".equalsIgnoreCase(mode) ? planIdProd : planIdTest;
    }
}
