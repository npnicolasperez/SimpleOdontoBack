package com.simpleodonto.profesional.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.simpleodonto.profesional.domain.Especialidad;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.profesional.dto.*;
import com.simpleodonto.profesional.repository.EspecialidadRepository;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
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

    private final ProfesionalRepository  profesionalRepository;
    private final EspecialidadRepository especialidadRepository;
    private final JwtUtil                jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier googleVerifier;

    @PostConstruct
    void init() {
        googleVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public AuthResponse register(RegisterRequest req) {
        if (profesionalRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        Especialidad especialidad = especialidadRepository.findById(req.especialidadId())
                .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada"));

        Profesional profesional = Profesional.builder()
                .nombre(req.nombre())
                .apellido(req.apellido())
                .email(req.email())
                .password(req.password())
                .matricula(req.matricula())
                .especialidad(especialidad)
                .perfilCompleto(true)
                .build();
        profesionalRepository.save(profesional);
        return toResponse(profesional);
    }

    public AuthResponse login(LoginRequest req) {
        Profesional profesional = profesionalRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (profesional.getPassword() == null || !req.password().equals(profesional.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        return toResponse(profesional);
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
        if (existente.isPresent()) return toResponse(existente.get());

        // Cuenta tradicional con el mismo email → vincular Google ID
        var porEmail = profesionalRepository.findByEmail(email);
        if (porEmail.isPresent()) {
            Profesional p = porEmail.get();
            p.setGoogleId(googleId);
            profesionalRepository.save(p);
            return toResponse(p);
        }

        // Primer login con Google → crear profesional con perfil incompleto
        String finalNombre  = nombre;
        String finalApellido = apellido;
        Profesional nuevo = Profesional.builder()
                .nombre(finalNombre)
                .apellido(finalApellido)
                .email(email)
                .googleId(googleId)
                .perfilCompleto(false)
                .build();
        profesionalRepository.save(nuevo);
        return toResponse(nuevo);
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
                p.getEspecialidad() != null ? p.getEspecialidad().getNombre() : null
        );
    }
}
