package com.simpleodonto.firma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FirmarRequest(
        @NotNull Long consultaId,
        @NotBlank @Size(max = 500_000) String pngBase64
) {}
