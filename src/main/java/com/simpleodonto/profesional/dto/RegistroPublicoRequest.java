package com.simpleodonto.profesional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroPublicoRequest(
        @NotBlank @Email String email,
        @NotBlank        String nombre,
        @NotBlank        String apellido,
        @NotBlank        String turnstileToken
) {}
