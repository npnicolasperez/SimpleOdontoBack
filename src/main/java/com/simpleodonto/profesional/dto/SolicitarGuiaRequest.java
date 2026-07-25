package com.simpleodonto.profesional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarGuiaRequest(
        @NotBlank @Email String email,
        @NotBlank        String whatsapp,
        @NotBlank        String turnstileToken
) {}
