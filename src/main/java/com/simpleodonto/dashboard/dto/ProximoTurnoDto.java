package com.simpleodonto.dashboard.dto;

import java.time.LocalDateTime;

public record ProximoTurnoDto(
        LocalDateTime fechaHora,
        String        pacienteNombre,
        String        pacienteApellido
) {}
