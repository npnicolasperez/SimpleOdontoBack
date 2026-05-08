package com.simpleodonto.profesional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotNull  Long especialidadId,
        String matricula,
        @NotBlank @Email String email,
        @NotBlank String password
) {}
