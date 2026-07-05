package com.simpleodonto.paciente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PacienteResponse(
        Long                          id,
        String                        nombre,
        String                        apellido,
        String                        dni,
        LocalDate                     fechaNac,
        String                        telefono,
        String                        email,
        String                        direccion,
        List<PacienteObraSocialDto>   obrasSociales,
        String                        ocupacion,
        String                        grupoSanguineo,
        String                        alergias,
        String                        medicaciones,
        String                        antecedentes,
        String                        antecedentesFamiliares,
        Double                        peso,
        Integer                       altura,
        LocalDateTime                 dateCreated,
        LocalDateTime                 lastUpdated,
        LocalDate                     ultimaVisita,
        LocalDate                     proximoTurno
) {}
