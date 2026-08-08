package com.simpleodonto.notification.service;

import com.simpleodonto.profesional.domain.PlanSolicitado;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.AdminEmails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.base-url}")
    private String baseUrl;

    // Precios de los planes — fuente de verdad del back. Cambiar acá si actualiza el pricing;
    // el front tiene su propia copia en App.jsx (constantes PLAN_MENSUAL_PRECIO / PLAN_ANUAL_PRECIO).
    private static final int PLAN_MENSUAL_PRECIO = 28_900;
    private static final int PLAN_ANUAL_PRECIO   = 21_900;
    private static final int PLAN_ANUAL_TOTAL    = PLAN_ANUAL_PRECIO * 12;
    private static final int PLAN_AHORRO_PCT     = Math.round((1f - (float) PLAN_ANUAL_PRECIO / PLAN_MENSUAL_PRECIO) * 100);
    private static String fmtPrecio(int n) { return String.format("%,d", n).replace(',', '.'); }

    /**
     * Avisa a los admins que un profesional se pre-registró. Trae los datos + un mensaje listo
     * para copiar y pegar en la respuesta al profesional (bienvenida + placeholder de link de pago
     * MP) + botones "Aprobar" y "Rechazar" que actúan con 1 click (tokens firmados 7 días).
     */
    @Async
    public void notifyNuevoPendiente(Profesional profesional, String tokenAprobar, String tokenRechazar) {
        var destinatarios = adminEmails.all();
        if (destinatarios.isEmpty()) {
            log.warn("Nuevo profesional PENDIENTE creado pero no hay admins configurados (ADMIN_EMAILS vacío)");
            return;
        }

        String subject          = "Nuevo registro pendiente — " + profesional.getNombre() + " " + profesional.getApellido();
        String googleConsoleUrl = "https://console.cloud.google.com/auth/audience?project=simpleodonto";
        String base             = baseUrl != null && !baseUrl.isBlank() ? baseUrl.replaceAll("/$", "") : "https://api.holadocapp.com";
        String urlAprobar       = base + "/api/auth/invitacion/aprobar?token="  + tokenAprobar;
        String urlRechazar      = base + "/api/auth/invitacion/rechazar?token=" + tokenRechazar;

        // Copy plan-específico. Si el registro es previo a esta feature (plan null), asumimos mensual.
        PlanSolicitado plan = profesional.getPlanSolicitado() != null ? profesional.getPlanSolicitado() : PlanSolicitado.MENSUAL;
        String planLabelAdmin;   // Para el panel del admin (con precio)
        String planFraseCobro;   // Para el mensaje al cliente ("... del plan mensual ($31.900)")
        String planBulletCobro;  // Bullet en "Cómo funciona la suscripción"
        switch (plan) {
            case ANUAL -> {
                planLabelAdmin  = "Anual — $" + fmtPrecio(PLAN_ANUAL_PRECIO) + "/mes ($" + fmtPrecio(PLAN_ANUAL_TOTAL) + " al año, pago único)";
                planFraseCobro  = "del plan anual ($" + fmtPrecio(PLAN_ANUAL_TOTAL) + " por 12 meses, ahorro de ~" + PLAN_AHORRO_PCT + "% vs mensual)";
                planBulletCobro = "Es un único pago por el año completo; no hay renovación automática hasta cumplir los 12 meses.";
            }
            case MENSUAL -> {
                planLabelAdmin  = "Mensual — $" + fmtPrecio(PLAN_MENSUAL_PRECIO) + "/mes";
                planFraseCobro  = "del plan mensual ($" + fmtPrecio(PLAN_MENSUAL_PRECIO) + " por mes)";
                planBulletCobro = "El primer débito automático se realiza recién el día 5 del mes siguiente, y a partir de ahí cada día 5 de cada mes.";
            }
            default -> {
                planLabelAdmin  = "No especificado";
                planFraseCobro  = "de la suscripción";
                planBulletCobro = "El primer débito automático se realiza recién el día 5 del mes siguiente, y a partir de ahí cada día 5 de cada mes.";
            }
        }

        // Mensaje listo para copiar/pegar en la respuesta manual al profesional
        String mensajeListo = """
            Hola %s!

            Bienvenido/a a HolaDoc. Recibimos tu solicitud y estamos armando tu cuenta.

            Para activarla, necesitamos que completes el pago %s desde el siguiente link:

            [COMPLETAR CON EL LINK DE PAGO DE MERCADO PAGO]

            Importante: una vez que hayas realizado el pago, te pedimos que respondas este mismo mail con un breve "Listo, pagué" (o similar). Queremos activar tu cuenta lo antes posible, sin tener que esperar la confirmación automática de Mercado Pago.

            Cómo funciona la suscripción:
            • Tenés 7 días de prueba gratis desde que activamos tu cuenta, para que puedas conocer todas las funcionalidades con tranquilidad.
            • %s
            • No hay ningún tipo de compromiso ni permanencia mínima: si en algún momento decidís no continuar, podés solicitar la baja respondiendo este mismo mail y cancelamos la suscripción de inmediato.

            Cuando confirmemos tu pago vas a recibir un segundo mail avisándote que tu cuenta ya está activa, y desde ese momento podrás ingresar con tu cuenta de Google directamente en holadocapp.com.

            Cualquier duda o consulta, respondé este mismo mail — estamos a tu disposición durante todo el proceso.

            Saludos,
            El equipo de HolaDocApp
            holadocapp.com
            """.formatted(escape(profesional.getNombre()), planFraseCobro, planBulletCobro);

        String html = """
            <div style="font-family: sans-serif; max-width: 560px; margin: 0 auto;">
              <h2 style="color: #111; margin-bottom: 4px;">Nuevo registro pendiente</h2>
              <p style="color: #555; margin-top: 0;">Un profesional se registró. Copiá el mensaje de abajo para responderle, y cuando confirmes el pago vení acá y clickeá <strong>Aprobar</strong>.</p>

              <table style="border-collapse: collapse; margin-top: 16px;">
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Nombre:</td><td><strong>%s %s</strong></td></tr>
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Email:</td><td><a href="mailto:%s" style="color: #111; font-weight: 600;">%s</a></td></tr>
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Plan solicitado:</td><td><strong style="color: #111;">%s</strong></td></tr>
              </table>

              <div style="margin-top: 24px; padding: 16px; background: #f5f5f4; border: 1px solid #e7e5e4; border-radius: 8px;">
                <div style="font-weight: 600; color: #111; font-size: 13px; margin-bottom: 10px;">Mensaje listo para copiar y pegar</div>
                <pre style="margin: 0; padding: 14px 16px; background: #fff; border: 1px solid #e7e5e4; border-radius: 6px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 14px; line-height: 1.6; color: #111; white-space: pre-wrap; word-break: break-word;">%s</pre>
                <div style="margin-top: 10px; font-size: 11px; color: #888;">Tip: seleccionalo todo (⌘A / Ctrl+A) y copiá. Reemplazá el placeholder por el link real de MP.</div>
              </div>

              <div style="margin-top: 28px; padding: 20px; background: #fff; border: 1px solid #e7e5e4; border-radius: 8px; text-align: center;">
                <div style="font-weight: 600; color: #111; font-size: 14px; margin-bottom: 6px;">Cuando confirmes el pago</div>
                <p style="color: #555; font-size: 13px; margin: 0 0 16px; line-height: 1.5;">Clickeá <strong>Aprobar</strong> y la cuenta queda activa al instante. Si finalmente no vas a habilitarla, usá <strong>Rechazar</strong> para descartar el registro.</p>
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin: 0 auto;"><tr>
                  <td style="padding-right: 8px;">
                    <a href="%s" style="display: inline-block; background: #16a34a; color: #fff; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 14px;">✓ Aprobar cuenta</a>
                  </td>
                  <td style="padding-left: 8px;">
                    <a href="%s" style="display: inline-block; background: #fff; color: #dc2626; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 14px; border: 1.5px solid #dc2626;">✕ Rechazar</a>
                  </td>
                </tr></table>
                <div style="margin-top: 12px; font-size: 11px; color: #888;">Los botones expiran en 7 días.</div>
              </div>

              <div style="margin-top: 24px; padding: 16px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px;">
                <div style="font-weight: 600; color: #92400e; font-size: 13px; margin-bottom: 8px;">⚠️ Recordatorio: Google Calendar test user</div>
                <p style="color: #555; font-size: 12px; line-height: 1.5; margin: 0 0 10px;">
                  Mientras la app esté en modo Testing en Google, para que este profesional pueda conectar Google Calendar tenés que agregarlo como <strong>test user</strong>.
                </p>
                <p style="color: #555; font-size: 12px; line-height: 1.5; margin: 0 0 12px;">
                  Email a agregar: <code style="background: #fff; padding: 2px 6px; border-radius: 3px; border: 1px solid #fde68a;">%s</code>
                </p>
                <a href="%s" style="display: inline-block; background: #fff; color: #92400e; padding: 6px 12px; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 12px; border: 1px solid #fde68a;">Abrir Google Auth Platform →</a>
              </div>
            </div>
            """.formatted(
                escape(profesional.getNombre()),
                escape(profesional.getApellido()),
                escape(profesional.getEmail()),
                escape(profesional.getEmail()),
                escape(planLabelAdmin),
                escape(mensajeListo),
                escape(urlAprobar),
                escape(urlRechazar),
                escape(profesional.getEmail()),
                googleConsoleUrl);

        boolean ok = emailService.send(destinatarios.stream().toList(), subject, html);
        if (ok) {
            log.info("Notificación de nuevo PENDIENTE enviada a {} admin(s) — {}", destinatarios.size(), profesional.getEmail());
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
     * Mail al profesional cuando el admin aprueba su cuenta desde el mail (botón "Aprobar").
     * Le avisa que confirmamos el pago y que ya puede loguearse con Google. Es el mail que la
     * pantalla de éxito del registro le promete al profesional.
     */
    @Async
    public void notifyProfesionalAprobado(Profesional profesional) {
        String subject = "Tu cuenta de HolaDocApp está activa";
        String html = """
            <div style="font-family: sans-serif; max-width: 520px; margin: 0 auto;">
              <div style="text-align: center; margin-bottom: 24px;">
                <div style="width: 64px; height: 64px; border-radius: 50%%; background: #16a34a; margin: 0 auto 16px; display: inline-flex; align-items: center; justify-content: center; color: #fff; font-size: 32px; font-weight: 700; line-height: 64px;">✓</div>
                <h2 style="color: #111; margin: 0;">Confirmamos tu pago — tu cuenta está lista</h2>
              </div>
              <p style="color: #555; line-height: 1.65; font-size: 15px;">
                Hola %s! Recibimos y confirmamos tu pago de la suscripción mensual. Tu cuenta de HolaDoc ya está activa y podés empezar a usarla ahora mismo.
              </p>
              <p style="color: #555; line-height: 1.65; font-size: 15px;">
                Para ingresar, andá a <a href="https://holadocapp.com" style="color: #111; font-weight: 600;">holadocapp.com</a> y hacé click en "Iniciar sesión con Google". La primera vez que entres te vamos a pedir que completes tu especialidad y listo — ya podés arrancar a cargar pacientes, turnos y consultas.
              </p>
              <div style="margin: 28px 0; text-align: center;">
                <a href="https://holadocapp.com" style="display: inline-block; background: #111; color: #fff; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 14px;">Ingresar a HolaDoc →</a>
              </div>
              <p style="color: #555; line-height: 1.65; font-size: 14px;">
                Cualquier duda que te surja mientras vas conociendo el sistema, respondé este mismo mail — estamos a tu disposición durante todo el proceso.
              </p>
              <p style="margin-top: 24px; color: #555; line-height: 1.5; font-size: 14px;">
                Saludos,<br>
                <strong>El equipo de HolaDocApp</strong><br>
                <span style="color: #888;">holadocapp.com</span>
              </p>
            </div>
            """.formatted(escape(profesional.getNombre()));

        boolean ok = emailService.send(List.of(profesional.getEmail()), subject, html);
        if (ok) {
            log.info("Mail de aprobación enviado al profesional {}", profesional.getEmail());
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

        String subject = "Nuevo lead — quiere probar HolaDoc";

        // Mensaje "listo para pegar" que el admin manda al lead (por mail o WhatsApp).
        // Va sin caracteres especiales de HTML para que copie limpio.
        String mensajeListo = """
            Hola!

            Gracias por tu interés en HolaDoc. Te compartimos un link para que puedas conocer el sistema sin registrarte y sin necesidad de instalar nada:

            %s

            Allí vas a encontrar dos formas de conocerlo:

            → Demo interactiva (recomendado)
              Ingresás directamente a la aplicación con datos de ejemplo cargados. Podés navegar, registrar una consulta, crear un turno y trazar sobre una radiografía — todo funcionando de verdad, no es un video. En cuestión de minutos ya estás dentro del sistema.

            → Guía visual
              Si preferís ver un recorrido antes de utilizarlo, encontrarás un tour con capturas de cada pantalla y una breve explicación de sus funcionalidades.

            Podés comenzar por la opción que prefieras, sin ningún compromiso.

            Ante cualquier consulta, respondé este mismo mail y te contestamos a la brevedad. Si querés que te lo mostremos en vivo por videollamada, coordinamos el horario que mejor te acomode.

            Saludos,
            El equipo de HolaDocApp
            """.formatted(linkGuia);

        String html = """
            <div style="font-family: sans-serif; max-width: 560px; margin: 0 auto;">
              <h2 style="color: #111;">Nuevo lead — quiere probar HolaDoc</h2>
              <p style="color: #555;">Un profesional pidió conocer HolaDoc desde la pantalla de login. Contactalo por mail o WhatsApp copiando el mensaje de abajo.</p>
              <table style="border-collapse: collapse; margin-top: 12px;">
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">Email:</td><td><a href="mailto:%s" style="color: #111; font-weight: 600;">%s</a></td></tr>
                <tr><td style="padding: 4px 12px 4px 0; color: #888;">WhatsApp:</td><td><a href="https://wa.me/%s" style="color: #111; font-weight: 600;">%s</a></td></tr>
              </table>

              <div style="margin-top: 28px; padding: 16px; background: #f5f5f4; border: 1px solid #e7e5e4; border-radius: 8px;">
                <div style="font-weight: 600; color: #111; font-size: 13px; margin-bottom: 10px;">Mensaje listo para copiar y pegar</div>
                <pre style="margin: 0; padding: 14px 16px; background: #fff; border: 1px solid #e7e5e4; border-radius: 6px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 14px; line-height: 1.6; color: #111; white-space: pre-wrap; word-break: break-word;">%s</pre>
                <div style="margin-top: 10px; font-size: 11px; color: #888;">Tip: seleccionalo todo (⌘A / Ctrl+A) y copiá (⌘C / Ctrl+C).</div>
              </div>
            </div>
            """.formatted(
                escape(email), escape(email),
                escape(whatsapp.replaceAll("[^0-9+]", "")),
                escape(whatsapp),
                escape(mensajeListo));

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
