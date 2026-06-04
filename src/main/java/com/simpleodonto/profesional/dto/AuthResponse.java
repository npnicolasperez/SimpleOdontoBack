package com.simpleodonto.profesional.dto;

public record AuthResponse(
        String token,
        String email,
        String nombre,
        String apellido,
        boolean perfilCompleto,
        String especialidadNombre,
        boolean esAdmin
) {}
