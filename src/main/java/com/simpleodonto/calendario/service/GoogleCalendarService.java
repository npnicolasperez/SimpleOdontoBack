package com.simpleodonto.calendario.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import com.simpleodonto.turno.domain.Turno;
import com.simpleodonto.turno.repository.TurnoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @jakarta.annotation.PostConstruct
    private void trimUrls() {
        baseUrl    = baseUrl.trim();
        frontendUrl = frontendUrl.trim();
    }

    private static final String REDIRECT_URI_PATH = "/api/calendar/callback";
    private static final String APPLICATION_NAME  = "HelloDoc";
    private static final long   STATE_TTL_MS      = 10L * 60 * 1000;
    // Los LocalDateTime en la DB representan hora local del profesional (Argentina).
    // No usamos ZoneId.systemDefault() porque el contenedor de Railway corre en UTC.
    private static final ZoneId ZONA_PROFESIONAL  = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ProfesionalRepository profesionalRepository;
    private final TurnoRepository       turnoRepository;

    // state OAuth → { profesionalId, creadoMs } — solo vive en memoria hasta que se consume
    private final ConcurrentHashMap<String, StateEntry> oauthStates = new ConcurrentHashMap<>();
    private record StateEntry(Long profesionalId, long creadoMs) {}

    // ── OAuth ──────────────────────────────────────────────────────────────

    public String getAuthorizationUrl(Profesional profesional) throws Exception {
        purgarStatesExpirados();
        String state = UUID.randomUUID().toString();
        oauthStates.put(state, new StateEntry(profesional.getId(), System.currentTimeMillis()));

        GoogleAuthorizationCodeFlow flow = buildFlow();
        return flow.newAuthorizationUrl()
                .setRedirectUri(baseUrl + REDIRECT_URI_PATH)
                .setState(state)
                .set("login_hint", profesional.getEmail())
                .build();
    }

    @Transactional
    public String handleCallback(String code, String state) throws Exception {
        StateEntry entry = oauthStates.remove(state);   // single-use
        if (entry == null || System.currentTimeMillis() - entry.creadoMs() > STATE_TTL_MS) {
            throw new IllegalArgumentException("State OAuth inválido o expirado");
        }
        Long profesionalId = entry.profesionalId();

        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId, clientSecret, code,
                baseUrl + REDIRECT_URI_PATH
        ).execute();

        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profesional no encontrado"));

        profesional.setGoogleCalendarRefreshToken(tokenResponse.getRefreshToken());
        profesionalRepository.save(profesional);

        try {
            registrarWebhook(profesional, tokenResponse.getRefreshToken());
        } catch (Exception e) {
            log.warn("No se pudo registrar webhook de Google Calendar (normal en entorno local): {}", e.getMessage());
        }

        return frontendUrl + "?calendarConectado=true";
    }

    public String getFrontendUrl() { return frontendUrl; }

    public boolean estaConectado(Profesional profesional) {
        return profesional.getGoogleCalendarRefreshToken() != null
                && !profesional.getGoogleCalendarRefreshToken().isBlank();
    }

    // ── Eventos ────────────────────────────────────────────────────────────

    public String crearEvento(Profesional profesional, Turno turno) {
        if (!estaConectado(profesional)) return null;
        try {
            Calendar service = buildCalendarClient(profesional.getGoogleCalendarRefreshToken());
            Event event = buildEvent(turno);
            Event created = service.events().insert("primary", event).execute();
            return created.getId();
        } catch (Exception e) {
            manejarExcepcionToken(e, profesional);
            return null;
        }
    }

    public void actualizarEvento(Profesional profesional, Turno turno) {
        if (!estaConectado(profesional) || turno.getGoogleEventId() == null) return;
        try {
            Calendar service = buildCalendarClient(profesional.getGoogleCalendarRefreshToken());
            Event event = buildEvent(turno);
            service.events().update("primary", turno.getGoogleEventId(), event).execute();
        } catch (Exception e) {
            manejarExcepcionToken(e, profesional);
        }
    }

    public void eliminarEvento(Profesional profesional, String googleEventId) {
        if (!estaConectado(profesional) || googleEventId == null) return;
        try {
            Calendar service = buildCalendarClient(profesional.getGoogleCalendarRefreshToken());
            service.events().delete("primary", googleEventId).execute();
        } catch (Exception e) {
            manejarExcepcionToken(e, profesional);
        }
    }

    @Transactional
    public void desconectar(Profesional profesional) {
        profesional.setGoogleCalendarRefreshToken(null);
        profesional.setGoogleCalendarChannelId(null);
        profesional.setGoogleCalendarResourceId(null);
        profesional.setGoogleCalendarSyncToken(null);
        profesional.setGoogleCalendarWebhookExpiry(null);
        profesionalRepository.save(profesional);
    }

    private void manejarExcepcionToken(Exception e, Profesional profesional) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("invalid_grant") || msg.contains("Token has been expired or revoked")) {
            log.warn("Token de Google Calendar expirado o revocado para profesional {}. Se desconecta automáticamente.", profesional.getId());
            desconectar(profesional);
        } else {
            log.warn("Error con Google Calendar para profesional {}: {}", profesional.getId(), msg);
        }
    }

    // ── Webhook ────────────────────────────────────────────────────────────

    @Transactional
    public void procesarWebhook(String channelId, String resourceState) {
        log.info("[CalSync] Webhook recibido. channelId={}, resourceState={}", channelId, resourceState);
        if ("sync".equals(resourceState)) {
            log.info("[CalSync] Es webhook 'sync' inicial — se ignora.");
            return;
        }

        var optProf = profesionalRepository.findByGoogleCalendarChannelId(channelId);
        if (optProf.isEmpty()) {
            log.warn("[CalSync] No se encontró profesional con channelId={}", channelId);
            return;
        }
        Profesional profesional = optProf.get();
        log.info("[CalSync] Profesional encontrado: id={}, email={}", profesional.getId(), profesional.getEmail());
        try {
            sincronizarCambios(profesional);
        } catch (Exception e) {
            log.error("[CalSync] Error procesando webhook para profesional {}: {}", profesional.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    protected void sincronizarCambios(Profesional profesional) throws Exception {
        Calendar service = buildCalendarClient(profesional.getGoogleCalendarRefreshToken());

        String syncTokenInicial = profesional.getGoogleCalendarSyncToken();
        String pageToken = null;
        String nextSyncToken = null;
        int actualizados = 0, eliminados = 0, sinMatch = 0, totalItems = 0, paginas = 0;
        boolean syncTokenExpirado = false;

        do {
            Calendar.Events.List request = service.events().list("primary").setSingleEvents(true);

            if (pageToken != null) {
                request.setPageToken(pageToken);
            } else if (!syncTokenExpirado && syncTokenInicial != null) {
                request.setSyncToken(syncTokenInicial);
                if (paginas == 0) log.info("[CalSync] Usando syncToken existente para incremental sync.");
            } else {
                request.setTimeMin(new com.google.api.client.util.DateTime(System.currentTimeMillis()));
                if (paginas == 0) log.info("[CalSync] Sin syncToken válido; usando timeMin=now (full sync de futuros).");
            }

            Events events;
            try {
                events = request.execute();
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                if (e.getStatusCode() == 410) {
                    // syncToken expirado: limpiar y reintentar desde cero con timeMin=now
                    log.warn("[CalSync] syncToken expirado (410). Reiniciando sync con timeMin=now.");
                    syncTokenExpirado = true;
                    pageToken = null;
                    paginas = 0;
                    continue;
                }
                throw e;
            }

            paginas++;
            totalItems += events.getItems().size();
            for (Event event : events.getItems()) {
                var optTurno = turnoRepository.findByGoogleEventId(event.getId());
                if (optTurno.isEmpty()) {
                    sinMatch++;
                    continue;
                }
                Turno turno = optTurno.get();
                if ("cancelled".equals(event.getStatus())) {
                    turnoRepository.delete(turno);
                    eliminados++;
                    log.info("[CalSync] Turno id={} eliminado (event cancelado en Google).", turno.getId());
                } else if (event.getStart() != null && event.getStart().getDateTime() != null) {
                    LocalDateTime nuevaFecha = LocalDateTime.ofInstant(
                            new Date(event.getStart().getDateTime().getValue()).toInstant(),
                            ZONA_PROFESIONAL);
                    LocalDateTime fechaAnterior = turno.getFechaHora();
                    if (!nuevaFecha.equals(fechaAnterior)) {
                        turno.setFechaHora(nuevaFecha);
                        turnoRepository.save(turno);
                        actualizados++;
                        log.info("[CalSync] Turno id={} actualizado: {} → {}", turno.getId(), fechaAnterior, nuevaFecha);
                    }
                }
            }

            pageToken = events.getNextPageToken();
            // El syncToken sólo viene en la ÚLTIMA página
            if (pageToken == null) {
                nextSyncToken = events.getNextSyncToken();
            }
        } while (pageToken != null);

        log.info("[CalSync] Sync completo. páginas={}, items={}, actualizados={}, eliminados={}, sinMatch={}",
                paginas, totalItems, actualizados, eliminados, sinMatch);

        if (nextSyncToken != null) {
            profesional.setGoogleCalendarSyncToken(nextSyncToken);
            profesionalRepository.save(profesional);
            log.info("[CalSync] syncToken guardado para próximo incremental sync.");
        } else {
            log.warn("[CalSync] No se obtuvo nextSyncToken — próxima sync será full nuevamente.");
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void renovarWebhooksProximos() {
        long limite = System.currentTimeMillis() + (2L * 24 * 60 * 60 * 1000);
        profesionalRepository.findAll().stream()
                .filter(p -> p.getGoogleCalendarRefreshToken() != null
                        && p.getGoogleCalendarWebhookExpiry() != null
                        && p.getGoogleCalendarWebhookExpiry() < limite)
                .forEach(p -> {
                    try {
                        registrarWebhook(p, p.getGoogleCalendarRefreshToken());
                    } catch (Exception e) {
                        log.error("Error renovando webhook para profesional {}: {}", p.getId(), e.getMessage());
                    }
                });
    }

    // ── Internos ───────────────────────────────────────────────────────────

    private void registrarWebhook(Profesional profesional, String refreshToken) throws Exception {
        Calendar service = buildCalendarClient(refreshToken);
        String channelId = UUID.randomUUID().toString();

        Channel channel = new Channel()
                .setId(channelId)
                .setType("web_hook")
                .setAddress(baseUrl + "/api/calendar/webhook");

        Channel response = service.events().watch("primary", channel).execute();

        profesional.setGoogleCalendarChannelId(response.getId());
        profesional.setGoogleCalendarResourceId(response.getResourceId());
        profesional.setGoogleCalendarWebhookExpiry(response.getExpiration());
        profesional.setGoogleCalendarSyncToken(null);
        profesionalRepository.save(profesional);
    }

    private GoogleAuthorizationCodeFlow buildFlow() throws Exception {
        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId, clientSecret,
                List.of(CalendarScopes.CALENDAR)
        ).setAccessType("offline").setApprovalPrompt("force").build();
    }

    @SuppressWarnings("deprecation")
    private Calendar buildCalendarClient(String refreshToken) throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);
        credential.refreshToken();

        return new Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private void purgarStatesExpirados() {
        long ahora = System.currentTimeMillis();
        oauthStates.entrySet().removeIf(e -> ahora - e.getValue().creadoMs() > STATE_TTL_MS);
    }

    private Event buildEvent(Turno turno) {
        String titulo = turno.getPaciente() != null
                ? turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido()
                : (turno.getNombrePacienteLibre() != null ? turno.getNombrePacienteLibre() : "Turno");

        // El LocalDateTime del turno representa hora local del profesional (Argentina),
        // así que lo convertimos a UTC usando esa zona específica.
        long startMs = turno.getFechaHora().atZone(ZONA_PROFESIONAL).toInstant().toEpochMilli();
        long endMs   = turno.getFechaHora().plusMinutes(turno.getDuracionMinutos()).atZone(ZONA_PROFESIONAL).toInstant().toEpochMilli();

        return new Event()
                .setSummary(titulo)
                .setDescription(turno.getMotivo())
                .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startMs)))
                .setEnd(  new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endMs)));
    }
}
