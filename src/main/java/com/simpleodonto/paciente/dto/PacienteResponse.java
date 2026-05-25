package com.simpleodonto.paciente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteResponse(
        Long          id,
        String        nombre,
        String        apellido,
        String        dni,
        LocalDate     fechaNac,
        String        telefono,
        String        email,
        String        direccion,
        Long          obraSocialId,
        String        obraSocialNombre,
        String        nroAfiliado,
        String        planObraSocial,
        String        titularObraSocial,
        String        ocupacion,
        String        grupoSanguineo,
        String        alergias,
        String        medicaciones,
        String        antecedentes,
        String        antecedentesFamiliares,
        Double        peso,
        Integer       altura,
        LocalDateTime dateCreated,
        LocalDateTime lastUpdated
) {}
