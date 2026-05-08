package com.simpleodonto.shared.security;

import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil               jwtUtil;
    private final ProfesionalRepository profesionalRepository;

    public Profesional resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalStateException("Token no proporcionado");
        }
        String token = header.substring(7);
        if (!jwtUtil.isValid(token)) {
            throw new IllegalStateException("Token inválido o expirado");
        }
        String email = jwtUtil.extractEmail(token);
        return profesionalRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Profesional no encontrado"));
    }
}
