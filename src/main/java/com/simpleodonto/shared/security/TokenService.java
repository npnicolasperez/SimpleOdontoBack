package com.simpleodonto.shared.security;

import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import com.simpleodonto.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final ProfesionalRepository profesionalRepository;

    /**
     * Resuelve el Profesional autenticado. El JwtAuthenticationFilter guarda el email
     * como principal en el SecurityContext; acá lo reconstruimos a entidad.
     * El parámetro HttpServletRequest se mantiene por compatibilidad.
     */
    public Profesional resolve(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String email) || email.isBlank()) {
            throw new UnauthorizedException("No autenticado");
        }
        return profesionalRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Profesional no encontrado"));
    }
}
