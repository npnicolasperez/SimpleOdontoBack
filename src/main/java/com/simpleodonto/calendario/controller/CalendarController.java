package com.simpleodonto.calendario.controller;

import com.simpleodonto.calendario.service.GoogleCalendarService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final TokenService          tokenService;

    @GetMapping("/auth-url")
    public Map<String, String> getAuthUrl(HttpServletRequest request) throws Exception {
        Profesional profesional = tokenService.resolve(request);
        String url = googleCalendarService.getAuthorizationUrl(profesional);
        return Map.of("url", url);
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            String redirectUrl = googleCalendarService.handleCallback(code, state);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        } catch (Exception e) {
            log.error("Error en callback de Google Calendar: {}", e.getMessage());
            String errorUrl = googleCalendarService.getFrontendUrl() + "?calendarError=true";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl)).build();
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Goog-Channel-ID",    required = false) String channelId,
            @RequestHeader(value = "X-Goog-Resource-State",required = false) String resourceState) {
        if (channelId != null) {
            googleCalendarService.procesarWebhook(channelId, resourceState);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public Map<String, Boolean> status(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        // Validación activa contra Google. Si el token está revocado/expirado, el service
        // desconecta automáticamente y este endpoint devuelve conectado=false.
        return Map.of("conectado", googleCalendarService.estaConectadoYValido(profesional));
    }

    @DeleteMapping("/desconectar")
    public ResponseEntity<Void> desconectar(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        googleCalendarService.desconectar(profesional);
        return ResponseEntity.noContent().build();
    }
}
