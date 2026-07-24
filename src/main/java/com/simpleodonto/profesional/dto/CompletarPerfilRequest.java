package com.simpleodonto.profesional.dto;

import jakarta.validation.constraints.NotNull;

public record CompletarPerfilRequest(
        @NotNull(message = "La especialidad es obligatoria")
        Long especialidadId
) {}
