package com.simpleodonto.profesional.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.simpleodonto.finanzas.service.MedioPagoService;
import com.simpleodonto.pago.service.SuscripcionService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final ProfesionalRepository     profesionalRepository;
    private final EspecialidadRepository    especialidadRepository;
    private final JwtUtil                   jwtUtil;
    private final AdminNotificationService  adminNotification;
    private final AdminEmails               adminEmails;
    private final SuscripcionService        suscripcionService;
    private final MedioPagoService          medioPagoService;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Token de la URL de la guía pública. Debe coincidir con VITE_GUIA_TOKEN del front.
    // Para rotarlo: cambiar la env var GUIA_TOKEN en ambos servicios de Railway y redeploy.
    @Value("${app.guia-token:2f89a8b7-fde0-4fff-af9e-f63adcad8c68}")
    private String guiaToken;

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

        // Cuenta tradicional con el mismo email → vincular Google ID (primer login efectivo).
        var porEmail = profesionalRepository.findByEmail(email);
        if (porEmail.isPresent()) {
            Profesional p = porEmail.get();
            verificarEstado(p);
            p.setGoogleId(googleId);
            profesionalRepository.save(p);
            // Primer login → sembramos los medios de pago de sistema ("Efectivo" y "Transferencia").
            // Idempotente por si algo ya existe.
            medioPagoService.bootstrapSistema(p);
            return toResponse(p);
        }

        throw new IllegalArgumentException("Tu cuenta no está autorizada. Contactá al administrador.");
    }

    /**
     * Lead público — un profesional (aún no registrado) pidió que le mandemos la guía de uso.
     * Solo notifica al admin; no crea nada en DB. El admin le responde a mano con el link.
     */
    public void solicitarGuia(String email, String whatsapp) {
        String base = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl : "https://holadocapp.com";
        String linkGuia = base.replaceAll("/$", "") + "/bienvenida/" + guiaToken;
        adminNotification.notifyLeadGuia(email.trim(), whatsapp.trim(), linkGuia);
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

        // ─── MP redirect deshabilitado temporalmente ───
        // Para los primeros clientes, el admin responde manualmente al profesional con la bienvenida
        // + link de pago, y aprueba la cuenta a mano desde el mail (botón Aprobar) cuando ve que
        // entró el pago. Dejamos el código comentado para reactivarlo cuando escalemos.
        // String initPoint = suscripcionService.initPointPara(saved.getEmail()).orElse(null);
        // adminNotification.notifyProfesionalConLinkPago(saved, initPoint);

        // Generamos tokens firmados (7 días, purpose scoping) para los botones aprobar/rechazar
        // del mail del admin. El admin puede aprobar/rechazar con 1 click sin loguearse.
        String tokenAprobar  = jwtUtil.generateActionToken(saved.getEmail(), "invitacion-aprobar");
        String tokenRechazar = jwtUtil.generateActionToken(saved.getEmail(), "invitacion-rechazar");
        adminNotification.notifyNuevoPendiente(saved, tokenAprobar, tokenRechazar);
    }

    /**
     * Activa la cuenta desde el botón "Aprobar" del mail del admin. Valida el token firmado
     * (purpose scoping evita que se use un token de otro flujo) y activa al profesional.
     * Idempotente: si ya estaba activo, no rompe.
     */
    public Profesional aprobarInvitacion(String token) {
        String email = jwtUtil.parseActionToken(token, "invitacion-aprobar");
        Profesional p = profesionalRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Profesional no encontrado"));
        if (p.getEstado() != EstadoProfesional.ACTIVO) {
            p.setEstado(EstadoProfesional.ACTIVO);
            profesionalRepository.save(p);
            log.info("[aprobarInvitacion] Profesional {} activado desde el mail del admin", email);
            // Mail al profesional avisando que la cuenta está lista — esto es lo que le
            // prometimos en la pantalla de éxito del registro. Sólo se dispara la primera vez.
            adminNotification.notifyProfesionalAprobado(p);
        }
        return p;
    }

    /**
     * Rechaza la cuenta desde el botón "Rechazar" del mail del admin. Borra al profesional si
     * todavía está en PENDIENTE (para liberar el email). Si ya estaba activo o rechazado, no
     * hace nada — evita accidentes.
     */
    public Profesional rechazarInvitacion(String token) {
        String email = jwtUtil.parseActionToken(token, "invitacion-rechazar");
        Profesional p = profesionalRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Profesional no encontrado"));
        if (p.getEstado() == EstadoProfesional.PENDIENTE) {
            profesionalRepository.delete(p);
            log.info("[rechazarInvitacion] Profesional {} rechazado y borrado desde el mail del admin", email);
        }
        return p;
    }

    /**
     * Confirma el pago de un profesional pre-registrado. El front llama acá cuando MP redirige a
     * /post-pago con el preapproval_id en el query string. Validamos contra MP que la preapproval
     * realmente está autorizada — sin eso, alguien podría inventar pares (email, preapprovalId) y
     * activar cuentas sin pagar.
     */
    public boolean confirmarPago(String email, String preapprovalId) {
        if (email == null || email.isBlank() || preapprovalId == null || preapprovalId.isBlank()) {
            log.warn("[confirmarPago] email o preapprovalId vacíos");
            return false;
        }
        if (!suscripcionService.estaAutorizada(preapprovalId)) {
            log.warn("[confirmarPago] preapproval {} no autorizada — no activamos a {}", preapprovalId, email);
            return false;
        }
        var prof = profesionalRepository.findByEmail(email).orElse(null);
        if (prof == null) {
            log.warn("[confirmarPago] profesional con email {} no existe", email);
            return false;
        }
        // Guard anti-reuso: un mismo preapproval no puede activar más de una cuenta. Como MP no nos
        // manda external_reference, el preapproval_id (único y verificado como authorized contra MP)
        // es la única clave para evitar que un solo pago active varias cuentas distintas.
        var yaAsignada = profesionalRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
        if (yaAsignada != null && !yaAsignada.getId().equals(prof.getId())) {
            log.warn("[confirmarPago] preapproval {} ya está asignada a otro profesional ({}) — no activamos a {}",
                    preapprovalId, yaAsignada.getEmail(), email);
            return false;
        }
        if (prof.getEstado() != EstadoProfesional.ACTIVO) {
            prof.setEstado(EstadoProfesional.ACTIVO);
            prof.setMpPreapprovalId(preapprovalId);
            profesionalRepository.save(prof);
            log.info("[confirmarPago] Profesional {} activado vía confirmarPago (preapproval {})", email, preapprovalId);
        }
        return true;
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
