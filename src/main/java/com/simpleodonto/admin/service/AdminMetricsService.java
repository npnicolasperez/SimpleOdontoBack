package com.simpleodonto.admin.service;

import com.simpleodonto.admin.dto.AdminMetricsReport;
import com.simpleodonto.admin.dto.AdminMetricsReport.TopPro;
import com.simpleodonto.admin.dto.AdminMetricsReport.WebhookCaido;
import com.simpleodonto.notification.service.AdminNotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compila métricas de producción y envía el reporte por mail al admin.
 *
 * `enviarReporte()` es el único punto de entrada — corre automáticamente 1x/día a las 08:00 AR
 * y también se puede triggerar manualmente desde AdminMetricsController (mismo código, mismo mail).
 *
 * Queries: 6 en total (consolidamos 7 counts scalares en una sola query para reducir round-trips
 * al proxy de Postgres de Railway). Todas read-only en una sola transacción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMetricsService {

    @PersistenceContext
    private EntityManager em;

    private final AdminNotificationService adminNotification;

    /**
     * Punto de entrada único — genera métricas y envía el mail.
     * Corre por cron 1x/día a las 08:00 AR; también lo llama el controller para trigger manual.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "America/Argentina/Buenos_Aires")
    public void enviarReporte() {
        log.info("[AdminMetrics] Generando reporte de métricas");
        AdminMetricsReport metricas = generarMetricas();
        adminNotification.notifyDailyStats(metricas);
    }

    @Transactional(readOnly = true)
    public AdminMetricsReport generarMetricas() {
        // ── QUERY 1: profesionales por estado + emails ─────────────────
        long activos = 0, pendientes = 0, suspendidos = 0;
        List<String> mailsA = new ArrayList<>(), mailsP = new ArrayList<>(), mailsS = new ArrayList<>();
        List<Object[]> profRows = em.createNativeQuery("""
            SELECT COALESCE(estado, 'DESCONOCIDO'), COUNT(*),
                   STRING_AGG(email, '||' ORDER BY id)
            FROM profesional GROUP BY estado
        """).getResultList();
        for (Object[] r : profRows) {
            String estado = (String) r[0];
            long cant     = ((Number) r[1]).longValue();
            String emails = (String) r[2];
            List<String> lista = emails == null || emails.isBlank()
                    ? Collections.emptyList()
                    : List.of(emails.split("\\|\\|"));
            switch (estado) {
                case "ACTIVO"     -> { activos     = cant; mailsA = lista; }
                case "PENDIENTE"  -> { pendientes  = cant; mailsP = lista; }
                case "SUSPENDIDO" -> { suspendidos = cant; mailsS = lista; }
                default -> log.warn("[AdminMetrics] estado profesional inesperado: {}", estado);
            }
        }

        // ── QUERY 2 (CONSOLIDADA): 7 counts scalares en un solo round-trip ─
        // Postgres corre los subqueries en paralelo internamente.
        Object[] scalares = (Object[]) em.createNativeQuery("""
            SELECT
              (SELECT COUNT(*) FROM paciente)                                                     AS pacientes,
              (SELECT COUNT(*) FROM turnos)                                                       AS turnos,
              (SELECT COUNT(*) FROM egreso)                                                       AS egresos,
              (SELECT COUNT(*) FROM estudios)                                                     AS estudios,
              (SELECT COUNT(*) FROM consulta_archivo)                                             AS arch_cant,
              (SELECT COALESCE(SUM(LENGTH(data)), 0) FROM consulta_archivo)                       AS arch_bytes,
              (SELECT COUNT(*) FROM profesional WHERE google_calendar_refresh_token IS NOT NULL)  AS gcal_conectados
        """).getSingleResult();
        long pacientes      = ((Number) scalares[0]).longValue();
        long turnos         = ((Number) scalares[1]).longValue();
        long egresos        = ((Number) scalares[2]).longValue();
        long estudios       = ((Number) scalares[3]).longValue();
        long archCant       = ((Number) scalares[4]).longValue();
        long archBytes      = ((Number) scalares[5]).longValue();
        long gcalConectados = ((Number) scalares[6]).longValue();
        double archMb = Math.round((archBytes / 1024.0 / 1024.0) * 100.0) / 100.0;

        // ── QUERY 3: consultas por tipo_pago ──────────────────────────
        long consultasTotal = 0, consultasPart = 0, consultasOs = 0;
        List<Object[]> conRows = em.createNativeQuery("""
            SELECT COALESCE(tipo_pago, 'SIN_DATO'), COUNT(*) FROM consulta GROUP BY tipo_pago
        """).getResultList();
        for (Object[] r : conRows) {
            String tp = (String) r[0];
            long c    = ((Number) r[1]).longValue();
            consultasTotal += c;
            if      ("PARTICULAR".equals(tp))  consultasPart = c;
            else if ("OBRA_SOCIAL".equals(tp)) consultasOs   = c;
        }

        // ── QUERY 4: ingresos por estado ──────────────────────────────
        long ingConf = 0, ingPend = 0;
        List<Object[]> ingRows = em.createNativeQuery("""
            SELECT estado, COUNT(*) FROM ingreso GROUP BY estado
        """).getResultList();
        for (Object[] r : ingRows) {
            String est = (String) r[0];
            long c     = ((Number) r[1]).longValue();
            if      ("CONFIRMADO".equals(est)) ingConf = c;
            else if ("PENDIENTE".equals(est))  ingPend = c;
        }
        long ingresosTotal = ingConf + ingPend;

        // ── QUERY 5: webhooks caídos ──────────────────────────────────
        List<WebhookCaido> whCaidos = new ArrayList<>();
        List<Object[]> whRows = em.createNativeQuery("""
            SELECT email,
                   CASE WHEN google_calendar_webhook_expiry IS NULL THEN NULL
                        ELSE to_timestamp(google_calendar_webhook_expiry/1000)::date
                   END
            FROM profesional
            WHERE google_calendar_refresh_token IS NOT NULL
              AND (google_calendar_webhook_expiry IS NULL
                   OR google_calendar_webhook_expiry < EXTRACT(EPOCH FROM NOW()) * 1000)
            ORDER BY email
        """).getResultList();
        for (Object[] r : whRows) {
            String email  = (String) r[0];
            Date sqlDate  = (Date) r[1];
            LocalDate dia = sqlDate != null ? sqlDate.toLocalDate() : null;
            whCaidos.add(new WebhookCaido(email, dia));
        }

        // ── QUERY 6: acciones por profesional ACTIVO en últimos 7 días ─
        List<String> activos7d   = new ArrayList<>();
        List<String> inactivos7d = new ArrayList<>();
        List<TopPro> top5        = new ArrayList<>();
        List<Object[]> actRows = em.createNativeQuery("""
            SELECT p.email,
              (SELECT COUNT(*) FROM paciente WHERE profesional_id = p.id AND date_created >= NOW() - INTERVAL '7 days') +
              (SELECT COUNT(*) FROM consulta WHERE profesional_id = p.id AND date_created >= NOW() - INTERVAL '7 days') +
              (SELECT COUNT(*) FROM turnos   WHERE profesional_id = p.id AND date_created >= NOW() - INTERVAL '7 days') +
              (SELECT COUNT(*) FROM ingreso  WHERE profesional_id = p.id AND date_created >= NOW() - INTERVAL '7 days') +
              (SELECT COUNT(*) FROM egreso   WHERE profesional_id = p.id AND date_created >= NOW() - INTERVAL '7 days') +
              (SELECT COUNT(*) FROM estudios WHERE profesional_id = p.id AND date_created >= NOW() - INTERVAL '7 days')
              AS acciones
            FROM profesional p
            WHERE p.estado = 'ACTIVO'
            ORDER BY acciones DESC, p.email
        """).getResultList();
        for (Object[] r : actRows) {
            String email = (String) r[0];
            long acc     = ((Number) r[1]).longValue();
            if (acc > 0) activos7d.add(email);
            else         inactivos7d.add(email);
            if (top5.size() < 5 && acc > 0) top5.add(new TopPro(email, acc));
        }

        return new AdminMetricsReport(
                activos, pendientes, suspendidos, mailsA, mailsP, mailsS,
                pacientes,
                consultasTotal, consultasPart, consultasOs,
                turnos,
                ingresosTotal, ingConf, ingPend,
                egresos, estudios,
                archCant, archMb,
                gcalConectados, whCaidos,
                activos7d, inactivos7d, top5
        );
    }
}
