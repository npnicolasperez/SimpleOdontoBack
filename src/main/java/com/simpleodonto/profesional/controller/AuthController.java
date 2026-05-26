package com.simpleodonto.profesional.controller;

import com.simpleodonto.profesional.dto.*;
import com.simpleodonto.profesional.service.AuthService;
import com.simpleodonto.shared.security.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService  authService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginConGoogle(@RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(authService.loginConGoogle(req));
    }

    @PostMapping("/registro")
    public ResponseEntity<Void> registro(@Valid @RequestBody InvitarRequest req) {
        authService.invitar(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitar")
    public ResponseEntity<Void> invitar(
            @Valid @RequestBody InvitarRequest req,
            HttpServletRequest request) {
        tokenService.resolve(request);
        authService.invitar(req);
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
