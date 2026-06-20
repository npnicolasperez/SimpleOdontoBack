package com.simpleodonto.pago.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Valida la firma HMAC SHA256 que MercadoPago manda en los webhooks. Sin esto, cualquiera podría
 * pegarle a nuestro endpoint público con payloads inventados y activar cuentas sin pago.
 *
 * Tiene ambos secrets (test y prod). El caller (controller) le dice cuál usar según el endpoint
 * por el que llegó el webhook — /api/mp/webhook/test usa el secret test, /prod usa el secret prod.
 *
 * Docs: https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks
 * Manifest a firmar: "id:<DATA_ID>;request-id:<REQUEST_ID>;ts:<TS>;"
 *   - DATA_ID: viene del body como `data.id`.
 *   - REQUEST_ID: header x-request-id.
 *   - TS: timestamp del x-signature.
 * El v1 del x-signature es HMAC-SHA256(secret, manifest) en hex.
 */
@Service
@Slf4j
public class MercadoPagoWebhookValidator {

    private final String secretTest;
    private final String secretProd;

    public MercadoPagoWebhookValidator(
            @Value("${app.mp.webhook-secret-test}") String secretTest,
            @Value("${app.mp.webhook-secret-prod}") String secretProd) {
        this.secretTest = secretTest;
        this.secretProd = secretProd;
    }

    /**
     * Devuelve true si la firma es válida para el modo indicado. Si el secret del modo está vacío
     * (dev local sin config), loguea un warning y devuelve true para no bloquear el desarrollo —
     * en prod las env vars deben estar seteadas para que la validación sea real.
     */
    public boolean validar(String mode, String xSignature, String xRequestId, String dataId) {
        String secret = secretFor(mode);
        if (secret == null || secret.isBlank()) {
            log.warn("[MP/{}] Secret vacío — saltando validación de firma (modo dev)", mode);
            return true;
        }
        if (xSignature == null || xRequestId == null || dataId == null) {
            log.warn("[MP/{}] Webhook sin headers requeridos (x-signature, x-request-id) o sin data.id", mode);
            return false;
        }

        // x-signature viene como "ts=1700000000,v1=abc123..."
        String ts = null, v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("ts".equals(kv[0])) ts = kv[1];
            if ("v1".equals(kv[0])) v1 = kv[1];
        }
        if (ts == null || v1 == null) {
            log.warn("[MP/{}] x-signature mal formado: {}", mode, xSignature);
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        String calculado = hmacSha256(secret, manifest);

        boolean ok = constantTimeEquals(calculado, v1);
        if (!ok) {
            log.warn("[MP/{}] Firma inválida. Manifest={}, calc={}, recibido={}", mode, manifest, calculado, v1);
        }
        return ok;
    }

    private String secretFor(String mode) {
        return "prod".equalsIgnoreCase(mode) ? secretProd : secretTest;
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Error calculando HMAC", e);
        }
    }

    /** Comparación constante en tiempo para evitar timing attacks. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
