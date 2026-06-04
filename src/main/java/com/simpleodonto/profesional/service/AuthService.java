package com.simpleodonto.profesional.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.simpleodonto.profesional.domain.Especialidad;
import com.simpleodonto.profesional.domain.EstadoProfesional;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.notification.service.AdminNotificationService;
import com.simpleodonto.profesional.dto.*;
import com.simpleodonto.profesional.repository.EspecialidadRepository;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import com.simpleodonto.shared.security.AdminEmails;
import com.simpleodonto.shared.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ProfesionalRepository     profesionalRepository;
    private final EspecialidadRepository    especialidadRepository;
    private final JwtUtil                   jwtUtil;
    private final AdminNotificationService  adminNotification;
    private final AdminEmails               adminEmails;

    @Value("${google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier googleVerifier;

    @PostConstruct
    void init() {
        googleVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public AuthResponse loginConGoogle(GoogleAuthRequest req) {
        GoogleIdToken idToken;
        try {
            idToken = googleVerifier.verify(req.idToken());
        } catch (Exception e) {
            throw new IllegalArgumentException("Token de Google inválido");
        }
        if (idToken == null) throw new IllegalArgumentException("Token de Google inválido");

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId  = payload.getSubject();
        String email     = payload.getEmail();
        String nombre    = (String) payload.get("given_name");
        String apellido  = (String) payload.get("family_name");
        if (nombre  == null) nombre  = email.split("@")[0];
        if (apellido == null) apellido = "";

        // Usuario ya registrado con Google
        var existente = profesionalRepository.findByGoogleId(googleId);
        if (existente.isPresent()) {
            verificarEstado(existente.get());
            return toResponse(existente.get());
        }

        // Cuenta tradicional con el mismo email → vincular Google ID
        var porEmail = profesionalRepository.findByEmail(email);
        if (porEmail.isPresent()) {
            Profesional p = porEmail.get();
            verificarEstado(p);
            p.setGoogleId(googleId);
            profesionalRepository.save(p);
            return toResponse(p);
        }

        throw new IllegalArgumentException("Tu cuenta no está autorizada. Contactá al administrador.");
    }

    public void invitar(InvitarRequest req) {
        if (profesionalRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        Profesional nuevo = Profesional.builder()
                .nombre(req.nombre())
                .apellido(req.apellido())
                .email(req.email())
                .perfilCompleto(false)
                .estado(EstadoProfesional.PENDIENTE)
                .build();
        Profesional saved = profesionalRepository.save(nuevo);
        adminNotification.notifyNuevoPendiente(saved);
    }

    public void activar(String email) {
        Profesional p = profesionalRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Profesional no encontrado"));
        p.setEstado(EstadoProfesional.ACTIVO);
        profesionalRepository.save(p);
    }

    public void rechazar(String email) {
        profesionalRepository.findByEmail(email).ifPresent(p -> {
            if (p.getEstado() == EstadoProfesional.PENDIENTE) {
                profesionalRepository.delete(p);
            }
        });
    }

    private void verificarEstado(Profesional p) {
        if (p.getEstado() == null) return; // usuarios previos sin estado = ACTIVO
        if (p.getEstado() == EstadoProfesional.PENDIENTE) {
            throw new IllegalArgumentException("Tu cuenta está pendiente de activación.");
        }
        if (p.getEstado() == EstadoProfesional.SUSPENDIDO) {
            throw new IllegalArgumentException("Tu cuenta está suspendida. Contactá al administrador.");
        }
    }

    public AuthResponse completarPerfil(CompletarPerfilRequest req, Profesional profesional) {
        Especialidad especialidad = especialidadRepository.findById(req.especialidadId())
                .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada"));
        profesional.setEspecialidad(especialidad);
        profesional.setMatricula(req.matricula());
        profesional.setPerfilCompleto(true);
        profesionalRepository.save(profesional);
        return toResponse(profesional);
    }

    private AuthResponse toResponse(Profesional p) {
        return new AuthResponse(
                jwtUtil.generateToken(p.getEmail()),
                p.getEmail(), p.getNombre(), p.getApellido(),
                p.isPerfilCompleto(),
                p.getEspecialidad() != null ? p.getEspecialidad().getNombre() : null,
                adminEmails.isAdmin(p.getEmail())
        );
    }
}
