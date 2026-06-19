package com.simpleodonto.pago.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.simpleodonto.pago.service.MercadoPagoWebhookProcessor;
import com.simpleodonto.pago.service.MercadoPagoWebhookValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint público que recibe los webhooks de MercadoPago.
 * URL a configurar en el panel MP: {@code https://api.holadocapp.com/api/mp/webhook}.
 *
 * Importante: respondemos 200 SIEMPRE (salvo firma inválida → 401). MP reintenta si recibe
 * 4xx/5xx, así que cualquier error de procesamiento se loguea pero no lo propagamos —
 * si lo propagamos, MP reintenta el mismo evento N veces y eso solo amplifica el problema.
 */
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookValidator validator;
    private final MercadoPagoWebhookProcessor processor;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "x-signature",  required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody JsonNode body) {

        String type   = textOrNull(body, "type");
        String dataId = body.path("data").path("id").isMissingNode() ? null : body.path("data").path("id").asText();
        log.info("[MP] Webhook recibido: type={}, dataId={}", type, dataId);

        if (!validator.validar(xSignature, xRequestId, dataId)) {
            return ResponseEntity.status(401).build();
        }
        if (type == null || dataId == null) {
            log.warn("[MP] Webhook sin type o data.id — body={}", body);
            return ResponseEntity.ok().build();
        }

        try {
            processor.procesar(type, dataId);
        } catch (Exception e) {
            log.error("[MP] Error procesando webhook type={} dataId={}", type, dataId, e);
        }
        return ResponseEntity.ok().build();
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
