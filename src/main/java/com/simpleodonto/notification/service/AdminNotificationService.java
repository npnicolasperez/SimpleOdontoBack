package com.simpleodonto.notification.service;

import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.AdminEmails;
import com.simpleodonto.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servicio de alto nivel para notificar a los admins (vía email) sobre eventos del sistema.
 * Todos los métodos son @Async — los callers no esperan ni reciben confirmación de envío.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final EmailService emailService;
    private final AdminEmails  adminEmails;
    private final JwtUtil      jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void notifyNuevoPendiente(Profesional profesional) {
        var destinatarios = adminEmails.all();
        if (destinatarios.isEmpty()) {
            log.warn("Nuevo profesional PENDIENTE creado pero no hay admins configurados (ADMIN_EMAILS vacío)");
            return;
        }

        String approveToken = jwtUtil.generateActionToken(profesional.getEmail(), "approve");
        String rejectToken  = jwtUtil.generateActionToken(profesional.getEmail(), "reject");
        String approveUrl   = baseUrl + "/api/auth/aprobar?token="  + URLEncoder.encode(approveToken, StandardCharsets.UTF_8);
        String rejectUrl    = baseUrl + "/api/auth/rechazar?token=" + URLEncoder.encode(rejectToken,  StandardCharsets.UTF_8);

        String subject = "Nuevo registro pendiente — holaDoc";
        String html = """
            <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
              <h2 style="color: #111;">Nuevo profesional pendiente de activación</h2>
              <p style="color: #555;">Se registró un nuevo profesional que está esperando tu aprobación:</p>
              <table style="border-collapse: collapse; margin-top: 12px;">
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Nombre:</td><td><strong>%s %s</strong></td></tr>
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Email:</td><td>%s</td></tr>
              </table>
              <div style="margin-top: 28px; display: flex; gap: 12px;">
                <a href="%s" style="background: #111; color: #fff; padding: 10px 20px; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 14px;">Aprobar</a>
                <a href="%s" style="background: #fff; color: #111; padding: 10px 20px; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 14px; border: 1px solid #ddd;">Rechazar</a>
              </div>
              <p style="margin-top: 24px; color: #888; font-size: 12px;">
                Los links son válidos por 7 días. Si la cuenta ya fue procesada, los links no tienen efecto.
              </p>
            </div>
            """.formatted(
                escape(profesional.getNombre()),
                escape(profesional.getApellido()),
                escape(profesional.getEmail()),
                approveUrl,
                rejectUrl);

        boolean ok = emailService.send(destinatarios.stream().toList(), subject, html);
        if (ok) {
            log.info("Notificación de nuevo PENDIENTE enviada a {} admin(s)", destinatarios.size());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
