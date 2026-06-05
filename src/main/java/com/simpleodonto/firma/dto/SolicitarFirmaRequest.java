package com.simpleodonto.firma.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitarFirmaRequest(
        @NotNull Long consultaId
) {}
