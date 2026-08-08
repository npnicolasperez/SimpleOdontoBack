package com.simpleodonto.profesional.controller;

import com.simpleodonto.profesional.dto.*;
import com.simpleodonto.profesional.service.AuthService;
import com.simpleodonto.shared.security.TokenService;
import com.simpleodonto.shared.security.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService      authService;
    private final TokenService     tokenService;
    private final TurnstileService turnstileService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginConGoogle(@RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(authService.loginConGoogle(req));
    }

    @PostMapping("/registro")
    public ResponseEntity<Void> registro(@Valid @RequestBody RegistroPublicoRequest req) {
        if (!turnstileService.verify(req.turnstileToken())) {
            throw new IllegalArgumentException("Verificación anti-bot fallida");
        }
        authService.invitar(new InvitarRequest(req.email(), req.nombre(), req.apellido(), req.plan()));
        return ResponseEntity.ok().build();
    }

    /**
     * Lead público — un profesional no registrado pide la guía de uso. Solo dispara un mail al
     * admin con email + whatsapp para que el admin le responda a mano con el link a la doc.
     */
    @PostMapping("/solicitar-guia")
    public ResponseEntity<Void> solicitarGuia(@Valid @RequestBody SolicitarGuiaRequest req) {
        if (!turnstileService.verify(req.turnstileToken())) {
            throw new IllegalArgumentException("Verificación anti-bot fallida");
        }
        authService.solicitarGuia(req.email(), req.whatsapp());
        return ResponseEntity.ok().build();
    }

    /**
     * Llamado por el front desde /post-pago. Recibe el email del profesional + el preapproval_id
     * que MP devolvió en el query string del redirect. Valida con MP que la preapproval esté
     * authorized y activa la cuenta. Devuelve {"ok": true} si se activó (o ya estaba activa).
     */
    @PostMapping("/confirmar-pago")
    public ResponseEntity<java.util.Map<String, Boolean>> confirmarPago(@Valid @RequestBody ConfirmarPagoRequest req) {
        boolean ok = authService.confirmarPago(req.email(), req.preapprovalId());
        return ResponseEntity.ok(java.util.Map.of("ok", ok));
    }

    @PostMapping("/activar")
    public ResponseEntity<Void> activar(
            @RequestParam String email,
            HttpServletRequest request) {
        tokenService.resolve(request);
        authService.activar(email);
        return ResponseEntity.ok().build();
    }

    /**
     * Aprobar/rechazar una invitación desde el mail del admin. Sin login — el token firmado (7 días,
     * purpose scoping) valida la acción. Devuelve una HTML mínima porque la request viene desde un
     * click en un mail cliente, no desde la SPA.
     */
    @GetMapping(value = "/invitacion/aprobar", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> aprobarInvitacion(@RequestParam String token) {
        try {
            var p = authService.aprobarInvitacion(token);
            return ResponseEntity.ok(renderAccionOk(
                    "Cuenta aprobada",
                    "La cuenta de <strong>" + escapeHtml(p.getNombre()) + " " + escapeHtml(p.getApellido()) + "</strong> quedó activada. Ya puede loguearse con Google en <a href=\"https://holadocapp.com\">holadocapp.com</a>.",
                    "#16a34a", "✓"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(renderAccionError("No se pudo aprobar", e.getMessage()));
        }
    }

    @GetMapping(value = "/invitacion/rechazar", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> rechazarInvitacion(@RequestParam String token) {
        try {
            var p = authService.rechazarInvitacion(token);
            return ResponseEntity.ok(renderAccionOk(
                    "Registro rechazado",
                    "El registro de <strong>" + escapeHtml(p.getNombre()) + " " + escapeHtml(p.getApellido()) + "</strong> fue descartado. El email vuelve a estar disponible para un futuro registro.",
                    "#dc2626", "✕"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(renderAccionError("No se pudo rechazar", e.getMessage()));
        }
    }

    private static String renderAccionOk(String titulo, String mensajeHtml, String color, String icono) {
        return """
            <!doctype html><html lang="es"><head><meta charset="utf-8"><title>%s · HolaDoc</title><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f6f6f4; margin: 0; padding: 40px 20px; min-height: 100vh;">
              <div style="max-width: 480px; margin: 40px auto; background: #fff; border: 1px solid #e0e0dc; border-radius: 16px; padding: 40px 32px; text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.04);">
                <div style="width: 64px; height: 64px; border-radius: 50%%; background: %s; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32px; font-weight: 700; line-height: 1;">%s</div>
                <h1 style="margin: 0 0 12px; font-size: 22px; letter-spacing: -0.02em; color: #111;">%s</h1>
                <p style="margin: 0; font-size: 14px; color: #555; line-height: 1.6;">%s</p>
              </div>
            </body></html>
            """.formatted(titulo, color, icono, titulo, mensajeHtml);
    }

    private static String renderAccionError(String titulo, String detalle) {
        return """
            <!doctype html><html lang="es"><head><meta charset="utf-8"><title>%s · HolaDoc</title><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f6f6f4; margin: 0; padding: 40px 20px; min-height: 100vh;">
              <div style="max-width: 480px; margin: 40px auto; background: #fff; border: 1px solid #e0e0dc; border-radius: 16px; padding: 40px 32px; text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.04);">
                <div style="width: 64px; height: 64px; border-radius: 50%%; background: #dc2626; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32px; font-weight: 700; line-height: 1;">!</div>
                <h1 style="margin: 0 0 12px; font-size: 22px; letter-spacing: -0.02em; color: #111;">%s</h1>
                <p style="margin: 0; font-size: 14px; color: #555; line-height: 1.6;">%s</p>
                <p style="margin-top: 16px; font-size: 12px; color: #888;">Puede ser que el token haya expirado (7 días) o que la acción ya se haya ejecutado.</p>
              </div>
            </body></html>
            """.formatted(titulo, titulo, escapeHtml(detalle != null ? detalle : "Error desconocido"));
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @PutMapping("/completar-perfil")
    public ResponseEntity<AuthResponse> completarPerfil(
            @Valid @RequestBody CompletarPerfilRequest req,
            HttpServletRequest request) {
        var profesional = tokenService.resolve(request);
        return ResponseEntity.ok(authService.completarPerfil(req, profesional));
    }
}
