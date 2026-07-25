package com.simpleodonto.notification.service;

import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.AdminEmails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Notificaciones por email — admins y profesionales.
 * Todos los métodos son @Async — los callers no esperan ni reciben confirmación de envío.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final EmailService emailService;
    private final AdminEmails  adminEmails;

    /**
     * Avisa a los admins que un profesional se pre-registró. El admin no tiene que aprobar manualmente
     * — el flow es: el profesional recibe link de pago, paga, MP dispara webhook, la cuenta se activa
     * automáticamente. Este mail es solo informativo para tener visibilidad.
     */
    @Async
    public void notifyNuevoPendiente(Profesional profesional) {
        var destinatarios = adminEmails.all();
        if (destinatarios.isEmpty()) {
            log.warn("Nuevo profesional PENDIENTE creado pero no hay admins configurados (ADMIN_EMAILS vacío)");
            return;
        }

        String subject = "Nuevo registro pendiente de pago — HolaDocApp";
        String googleConsoleUrl = "https://console.cloud.google.com/auth/audience?project=simpleodonto";
        String html = """
            <div style="font-family: sans-serif; max-width: 520px; margin: 0 auto;">
              <h2 style="color: #111;">Nuevo profesional pre-registrado</h2>
              <p style="color: #555;">Se registró un profesional. Le enviamos el link de pago de la suscripción; cuando complete el pago, la cuenta se activa automáticamente.</p>
              <table style="border-collapse: collapse; margin-top: 12px;">
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Nombre:</td><td><strong>%s %s</strong></td></tr>
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Email:</td><td>%s</td></tr>
              </table>

              <div style="margin-top: 32px; padding: 16px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px;">
                <div style="font-weight: 600; color: #92400e; font-size: 14px; margin-bottom: 8px;">⚠️ Recordatorio: Google Calendar test user</div>
                <p style="color: #555; font-size: 13px; line-height: 1.5; margin: 0 0 12px;">
                  Mientras la app esté en modo Testing en Google, para que este profesional pueda conectar Google Calendar tenés que agregarlo como <strong>test user</strong>.
                </p>
                <p style="color: #555; font-size: 13px; line-height: 1.5; margin: 0 0 14px;">
                  Email a agregar: <code style="background: #fff; padding: 2px 6px; border-radius: 3px; border: 1px solid #fde68a;">%s</code>
                </p>
                <a href="%s" style="display: inline-block; background: #fff; color: #92400e; padding: 8px 14px; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 13px; border: 1px solid #fde68a;">Abrir Google Auth Platform →</a>
              </div>
            </div>
            """.formatted(
                escape(profesional.getNombre()),
                escape(profesional.getApellido()),
                escape(profesional.getEmail()),
                escape(profesional.getEmail()),
                googleConsoleUrl);

        boolean ok = emailService.send(destinatarios.stream().toList(), subject, html);
        if (ok) {
            log.info("Notificación de nuevo PENDIENTE enviada a {} admin(s)", destinatarios.size());
        }
    }

    /**
     * Mail de bienvenida al profesional con el link de suscripción de MP. El link viene del init_point
     * de la preapproval que creamos en AuthService.invitar. Cuando el profesional paga, MP dispara
     * webhook → activación automática.
     */
    @Async
    public void notifyProfesionalConLinkPago(Profesional profesional, String initPoint) {
        if (initPoint == null || initPoint.isBlank()) {
            log.warn("No se envía mail a {} — initPoint vacío (MP falló al crear preapproval)", profesional.getEmail());
            return;
        }
        String subject = "Bienvenido a HolaDocApp — Completá tu suscripción";
        String html = """
            <div style="font-family: sans-serif; max-width: 520px; margin: 0 auto;">
              <h2 style="color: #111;">Hola %s, bienvenido a HolaDocApp</h2>
              <p style="color: #555; line-height: 1.55;">
                Recibimos tu pre-registro. Para activar tu cuenta, completá la suscripción mensual desde el link de abajo.
                Cuando termines el pago, tu cuenta queda activa al instante y ya podés iniciar sesión con Google.
              </p>
              <div style="margin-top: 28px;">
                <a href="%s" style="display: inline-block; background: #111; color: #fff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 14px;">Completar suscripción</a>
              </div>
              <p style="margin-top: 28px; color: #888; font-size: 12px; line-height: 1.5;">
                Si el botón no abre, copiá y pegá este link en tu navegador:<br>
                <span style="word-break: break-all;">%s</span>
              </p>
              <p style="margin-top: 24px; color: #888; font-size: 12px; line-height: 1.5;">
                ¿No reconocés este registro? Ignorá este mail — no se va a crear la cuenta sin el pago.
              </p>
            </div>
            """.formatted(
                escape(profesional.getNombre()),
                initPoint,
                initPoint);

        boolean ok = emailService.send(List.of(profesional.getEmail()), subject, html);
        if (ok) {
            log.info("Mail con link de pago enviado a {}", profesional.getEmail());
        }
    }

    /**
     * Lead nuevo — un profesional (no registrado) pidió que le mandemos la guía de uso. Le pasa
     * mail + whatsapp; nosotros le respondemos a mano con el link de la doc y (opcionalmente)
     * seguimos el contacto por whatsapp.
     */
    @Async
    public void notifyLeadGuia(String email, String whatsapp, String linkGuia) {
        var destinatarios = adminEmails.all();
        if (destinatarios.isEmpty()) {
            log.warn("Lead de guía recibido pero no hay admins configurados (ADMIN_EMAILS vacío)");
            return;
        }

        String subject = "Nuevo lead — solicitó la guía de HolaDocApp";
        String html = """
            <div style="font-family: sans-serif; max-width: 520px; margin: 0 auto;">
              <h2 style="color: #111;">Nuevo lead — solicitó la guía</h2>
              <p style="color: #555;">Un profesional pidió la guía de uso desde la pantalla de login. Contactalo por mail o whatsapp con el link a la doc.</p>
              <table style="border-collapse: collapse; margin-top: 12px;">
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Email:</td><td><a href="mailto:%s" style="color: #111; font-weight: 600;">%s</a></td></tr>
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">WhatsApp:</td><td><a href="https://wa.me/%s" style="color: #111; font-weight: 600;">%s</a></td></tr>
              </table>

              <div style="margin-top: 28px; padding: 16px; background: #f5f5f4; border: 1px solid #e7e5e4; border-radius: 8px;">
                <div style="font-weight: 600; color: #111; font-size: 13px; margin-bottom: 8px;">Link a la guía (listo para copiar)</div>
                <code style="display: block; background: #fff; padding: 10px 12px; border-radius: 4px; border: 1px solid #e7e5e4; font-size: 13px; color: #111; word-break: break-all;">%s</code>
              </div>
            </div>
            """.formatted(
                escape(email), escape(email),
                escape(whatsapp.replaceAll("[^0-9+]", "")),
                escape(whatsapp),
                escape(linkGuia));

        boolean ok = emailService.send(destinatarios.stream().toList(), subject, html);
        if (ok) {
            log.info("Notificación de lead de guía enviada a {} admin(s) — email={}, whatsapp={}",
                    destinatarios.size(), email, whatsapp);
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
