package com.simpleodonto.profesional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConfirmarPagoRequest(
        @NotBlank @Email String email,
        @NotBlank        String preapprovalId
) {}
