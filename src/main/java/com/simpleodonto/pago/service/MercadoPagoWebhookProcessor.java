package com.simpleodonto.pago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.simpleodonto.profesional.domain.EstadoProfesional;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa los eventos del webhook de MercadoPago y actualiza el estado del profesional.
 *
 * Modelo de datos asumido (lo seteamos cuando creamos la preapproval):
 *   - external_reference = email del profesional. Es el puente entre MP y nuestra DB.
 *
 * Estados de preapproval que nos interesan:
 *   - "authorized": el usuario autorizó y el primer cobro está OK → ACTIVO.
 *   - "cancelled" / "paused": el usuario canceló → SUSPENDIDO.
 * Estados de authorized_payment (cobros mensuales recurrentes):
 *   - "approved": cobro del mes OK → mantener ACTIVO.
 *   - "rejected": cobro falló → SUSPENDIDO (después podemos hacer dunning si queremos).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookProcessor {

    private final MercadoPagoService     mpService;
    private final ProfesionalRepository  profesionalRepository;

    @Transactional
    public void procesar(String type, String dataId) {
        switch (type) {
            case "subscription_preapproval"          -> procesarPreapproval(dataId);
            case "subscription_authorized_payment"   -> procesarAuthorizedPayment(dataId);
            case "payment"                           -> procesarPayment(dataId);
            default -> log.info("[MP] Evento ignorado: type={}, dataId={}", type, dataId);
        }
    }

    /** Suscripción creada / actualizada. Lo más importante: status=authorized → activar profesional. */
    private void procesarPreapproval(String preapprovalId) {
        mpService.obtenerPreapproval(preapprovalId).ifPresentOrElse(node -> {
            String status      = textOrNull(node, "status");
            String externalRef = textOrNull(node, "external_reference");
            log.info("[MP] preapproval {} status={} external_reference={}", preapprovalId, status, externalRef);
            if (externalRef == null || externalRef.isBlank()) {
                log.warn("[MP] preapproval {} sin external_reference — no podemos vincular al profesional", preapprovalId);
                return;
            }
            switch (status) {
                case "authorized" -> activar(externalRef);
                case "cancelled", "paused" -> suspender(externalRef);
                default -> log.info("[MP] preapproval {} status={} — sin acción", preapprovalId, status);
            }
        }, () -> log.warn("[MP] No se pudo obtener preapproval {}", preapprovalId));
    }

    /** Cobro mensual de una suscripción. Solo nos importa para detectar fallas. */
    private void procesarAuthorizedPayment(String authorizedPaymentId) {
        mpService.obtenerAuthorizedPayment(authorizedPaymentId).ifPresentOrElse(node -> {
            String status     = textOrNull(node, "status");
            String externalRef = textOrNull(node, "external_reference");
            // En algunos esquemas el external_reference vive en el preapproval, no en el authorized_payment;
            // si no está, intentamos via preapproval.
            if (externalRef == null) {
                String preapprovalId = textOrNull(node, "preapproval_id");
                if (preapprovalId != null) {
                    externalRef = mpService.obtenerPreapproval(preapprovalId)
                            .map(n -> textOrNull(n, "external_reference"))
                            .orElse(null);
                }
            }
            log.info("[MP] authorized_payment {} status={} external_reference={}", authorizedPaymentId, status, externalRef);
            if (externalRef == null) return;
            if ("rejected".equals(status)) suspender(externalRef);
        }, () -> log.warn("[MP] No se pudo obtener authorized_payment {}", authorizedPaymentId));
    }

    /** Pago directo (no recurrente). Lo procesamos por si activamos cobro one-shot en el futuro. */
    private void procesarPayment(String paymentId) {
        mpService.obtenerPayment(paymentId).ifPresentOrElse(node -> {
            String status     = textOrNull(node, "status");
            String externalRef = textOrNull(node, "external_reference");
            log.info("[MP] payment {} status={} external_reference={}", paymentId, status, externalRef);
            if (externalRef == null || externalRef.isBlank()) return;
            if ("approved".equals(status)) activar(externalRef);
        }, () -> log.warn("[MP] No se pudo obtener payment {}", paymentId));
    }

    private void activar(String email) {
        profesionalRepository.findByEmail(email).ifPresentOrElse(p -> {
            if (p.getEstado() != EstadoProfesional.ACTIVO) {
                p.setEstado(EstadoProfesional.ACTIVO);
                profesionalRepository.save(p);
                log.info("[MP] Profesional {} activado por pago aprobado", email);
            }
        }, () -> log.warn("[MP] Profesional con email {} no encontrado al intentar activar", email));
    }

    private void suspender(String email) {
        profesionalRepository.findByEmail(email).ifPresent(p -> {
            if (p.getEstado() != EstadoProfesional.SUSPENDIDO) {
                p.setEstado(EstadoProfesional.SUSPENDIDO);
                profesionalRepository.save(p);
                log.info("[MP] Profesional {} suspendido por cancelación / cobro fallido", email);
            }
        });
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
