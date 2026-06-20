package com.simpleodonto.pago.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.simpleodonto.pago.service.MercadoPagoWebhookProcessor;
import com.simpleodonto.pago.service.MercadoPagoWebhookValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos que reciben los webhooks de MercadoPago. Hay dos paths separados (test y prod)
 * porque cada modo de MP es un universo aislado con sus propias credenciales (access token, webhook
 * secret) y plan id. El endpoint determina con qué modo se valida y se consulta.
 *
 * URLs a configurar en el panel MP:
 *   - App de prueba       → {@code https://api.holadocapp.com/api/mp/webhook/test}
 *   - App de producción   → {@code https://api.holadocapp.com/api/mp/webhook/prod}
 *
 * Importante: respondemos 200 SIEMPRE (salvo firma inválida → 401). MP reintenta si recibe 4xx/5xx,
 * así que cualquier error de procesamiento se loguea pero no se propaga — si lo propagamos, MP
 * reintenta el mismo evento N veces y solo amplifica el problema.
 */
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookValidator validator;
    private final MercadoPagoWebhookProcessor processor;

    @PostMapping("/webhook/test")
    public ResponseEntity<Void> webhookTest(
            @RequestHeader(value = "x-signature",  required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody JsonNode body) {
        return procesar("test", xSignature, xRequestId, body);
    }

    @PostMapping("/webhook/prod")
    public ResponseEntity<Void> webhookProd(
            @RequestHeader(value = "x-signature",  required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody JsonNode body) {
        return procesar("prod", xSignature, xRequestId, body);
    }

    private ResponseEntity<Void> procesar(String mode, String xSignature, String xRequestId, JsonNode body) {
        String type   = textOrNull(body, "type");
        String dataId = body.path("data").path("id").isMissingNode() ? null : body.path("data").path("id").asText();
        log.info("[MP/{}] Webhook recibido: type={}, dataId={}", mode, type, dataId);

        if (!validator.validar(mode, xSignature, xRequestId, dataId)) {
            return ResponseEntity.status(401).build();
        }
        if (type == null || dataId == null) {
            log.warn("[MP/{}] Webhook sin type o data.id — body={}", mode, body);
            return ResponseEntity.ok().build();
        }

        try {
            processor.procesar(mode, type, dataId);
        } catch (Exception e) {
            log.error("[MP/{}] Error procesando webhook type={} dataId={}", mode, type, dataId, e);
        }
        return ResponseEntity.ok().build();
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
