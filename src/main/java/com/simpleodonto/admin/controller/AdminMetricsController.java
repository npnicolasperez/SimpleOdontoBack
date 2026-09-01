package com.simpleodonto.admin.controller;

import com.simpleodonto.admin.service.AdminMetricsService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.AdminEmails;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Endpoints administrativos. Solo accesibles por profesionales cuyo email esté listado
 * en la env var ADMIN_EMAILS.
 */
@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
@Slf4j
public class AdminMetricsController {

    private final AdminMetricsService adminMetrics;
    private final TokenService        tokenService;
    private final AdminEmails         adminEmails;

    /**
     * Dispara manualmente el reporte diario de métricas al admin. Útil para testeo o para
     * pedir un snapshot on-demand sin esperar al cron de las 08:00.
     * Requiere login como admin (email en ADMIN_EMAILS).
     */
    @PostMapping("/enviar")
    public ResponseEntity<Map<String, String>> enviarReporteManual(HttpServletRequest request) {
        Profesional caller = tokenService.resolve(request);
        if (caller == null || !adminEmails.isAdmin(caller.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo admins pueden disparar este reporte");
        }
        log.info("[AdminMetrics] Trigger manual por admin: {}", caller.getEmail());
        try {
            adminMetrics.enviarReporte();
            return ResponseEntity.ok(Map.of(
                    "status",  "ok",
                    "message", "Reporte enviado a los admins configurados"
            ));
        } catch (Exception e) {
            log.error("[AdminMetrics] Error en trigger manual: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar/enviar el reporte: " + e.getMessage());
        }
    }
}
