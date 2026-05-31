package com.simpleodonto.profesional.controller;

import com.simpleodonto.profesional.dto.*;
import com.simpleodonto.profesional.service.AuthService;
import com.simpleodonto.shared.security.JwtUtil;
import com.simpleodonto.shared.security.TokenService;
import com.simpleodonto.shared.security.TurnstileService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService     authService;
    private final TokenService    tokenService;
    private final TurnstileService turnstileService;
    private final JwtUtil          jwtUtil;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginConGoogle(@RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(authService.loginConGoogle(req));
    }

    @PostMapping("/registro")
    public ResponseEntity<Void> registro(@Valid @RequestBody RegistroPublicoRequest req) {
        if (!turnstileService.verify(req.turnstileToken())) {
            throw new IllegalArgumentException("Verificación anti-bot fallida");
        }
        authService.invitar(new InvitarRequest(req.email(), req.nombre(), req.apellido()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/activar")
    public ResponseEntity<Void> activar(
            @RequestParam String email,
            HttpServletRequest request) {
        tokenService.resolve(request);
        authService.activar(email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/completar-perfil")
    public ResponseEntity<AuthResponse> completarPerfil(
            @Valid @RequestBody CompletarPerfilRequest req,
            HttpServletRequest request) {
        var profesional = tokenService.resolve(request);
        return ResponseEntity.ok(authService.completarPerfil(req, profesional));
    }

    /**
     * Endpoint público que se invoca desde el link del email enviado al admin.
     * Valida el token firmado (purpose=approve) y activa la cuenta.
     */
    @GetMapping(value = "/aprobar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> aprobar(@RequestParam String token) {
        try {
            String email = jwtUtil.parseActionToken(token, "approve");
            authService.activar(email);
            return ResponseEntity.ok(htmlOk("Cuenta aprobada", "El profesional <strong>" + escape(email) + "</strong> ya puede iniciar sesión."));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(400).body(htmlError("Link inválido o expirado", "Volvé al mail y verificá el link."));
        }
    }

    @GetMapping(value = "/rechazar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> rechazar(@RequestParam String token) {
        try {
            String email = jwtUtil.parseActionToken(token, "reject");
            authService.rechazar(email);
            return ResponseEntity.ok(htmlOk("Solicitud rechazada", "La solicitud de <strong>" + escape(email) + "</strong> fue eliminada."));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(400).body(htmlError("Link inválido o expirado", "Volvé al mail y verificá el link."));
        }
    }

    private static String htmlOk(String titulo, String detalle) {
        return htmlPage(titulo, detalle, "#10b981");
    }

    private static String htmlError(String titulo, String detalle) {
        return htmlPage(titulo, detalle, "#dc2626");
    }

    private static String htmlPage(String titulo, String detalle, String accent) {
        return """
            <!DOCTYPE html>
            <html lang="es"><head>
              <meta charset="UTF-8"><title>%s — holaDoc</title>
              <style>
                body { font-family: system-ui, sans-serif; background: #fafafa; color: #111;
                       display: flex; align-items: center; justify-content: center;
                       min-height: 100vh; margin: 0; }
                .card { background: white; border-radius: 12px; padding: 40px 48px;
                        box-shadow: 0 2px 24px rgba(0,0,0,0.06); max-width: 420px; text-align: center; }
                .dot  { width: 12px; height: 12px; border-radius: 50%%; background: %s; display: inline-block; margin-bottom: 16px; }
                h1    { font-size: 22px; margin: 0 0 12px; }
                p     { color: #555; margin: 0; line-height: 1.5; }
              </style>
            </head><body>
              <div class="card">
                <div class="dot"></div>
                <h1>%s</h1>
                <p>%s</p>
              </div>
            </body></html>
            """.formatted(escape(titulo), accent, escape(titulo), detalle);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
