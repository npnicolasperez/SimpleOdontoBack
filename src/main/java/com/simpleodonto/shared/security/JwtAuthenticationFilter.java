package com.simpleodonto.shared.security;

import com.simpleodonto.profesional.domain.EstadoProfesional;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.profesional.repository.ProfesionalRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil               jwtUtil;
    private final ProfesionalRepository profesionalRepository;
    private final AdminEmails           adminEmails;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (JwtException | IllegalArgumentException e) {
            sendUnauthorized(res, "Token inválido o expirado");
            return;
        }

        Profesional profesional = profesionalRepository.findByEmail(email).orElse(null);
        if (profesional == null) {
            sendUnauthorized(res, "Profesional no encontrado");
            return;
        }
        if (profesional.getEstado() == EstadoProfesional.SUSPENDIDO) {
            sendUnauthorized(res, "Cuenta suspendida");
            return;
        }

        // Principal = email (String). Evita serializar la entidad JPA en el SecurityContext.
        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_PROFESIONAL"));
        if (adminEmails.isAdmin(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }

    private void sendUnauthorized(HttpServletResponse res, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"error\":\"" + msg + "\"}");
    }
}
