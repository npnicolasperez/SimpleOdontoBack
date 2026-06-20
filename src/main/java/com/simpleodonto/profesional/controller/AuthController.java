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
}
