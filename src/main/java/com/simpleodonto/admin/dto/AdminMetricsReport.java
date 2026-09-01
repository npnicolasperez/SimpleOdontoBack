package com.simpleodonto.admin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Snapshot de métricas de producción para el reporte diario del admin.
 * Se genera desde AdminMetricsService.generarMetricas() y se envía por mail vía
 * AdminNotificationService.notifyDailyStats().
 */
public record AdminMetricsReport(
        // ── Volumen ─────────────────────────────────────────────
        long profActivos,
        long profPendientes,
        long profSuspendidos,
        List<String> mailsActivos,
        List<String> mailsPendientes,
        List<String> mailsSuspendidos,
        long pacientes,
        long consultasTotal,
        long consultasParticulares,
        long consultasObraSocial,
        long turnos,
        long ingresosTotal,
        long ingresosConfirmados,
        long ingresosPendientes,
        long egresos,
        long estudios,
        long archivosCant,
        double archivosMb,

        // ── Integraciones ───────────────────────────────────────
        long gcalConectados,
        List<WebhookCaido> webhooksCaidos,

        // ── Actividad últimos 7 días ────────────────────────────
        List<String> activos7d,
        List<String> inactivos7d,
        List<TopPro>  top5Semana
) {
    public record WebhookCaido(String email, LocalDate vencidoDesde) {}
    public record TopPro(String email, long acciones) {}
}
