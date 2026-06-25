package com.simpleodonto.pago.service;

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

    private final String mode;
    private final String planIdTest;
    private final String planIdProd;

    public SuscripcionService(@Value("${app.mp.mode}")                     String mode,
                              @Value("${app.mp.preapproval-plan-id-test}") String planIdTest,
                              @Value("${app.mp.preapproval-plan-id-prod}") String planIdProd) {
        this.mode       = mode;
        this.planIdTest = planIdTest;
        this.planIdProd = planIdProd;
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
