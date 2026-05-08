package com.simpleodonto.analisis.service;

import com.simpleodonto.analisis.domain.Analisis;
import com.simpleodonto.analisis.dto.AnalisisDetalleResponse;
import com.simpleodonto.analisis.dto.AnalisisResponse;
import com.simpleodonto.analisis.dto.AnalisisUpdateRequest;
import com.simpleodonto.analisis.repository.AnalisisRepository;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalisisService {

    private final AnalisisRepository analisisRepository;
    private final PacienteRepository pacienteRepository;

    public List<AnalisisResponse> listar(Profesional profesional) {
        return analisisRepository.findByProfesionalIdOrderByLastUpdatedDesc(profesional.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<AnalisisResponse> listarPorPaciente(Long pacienteId, Profesional profesional) {
        return analisisRepository.findByProfesionalIdAndPacienteIdOrderByLastUpdatedDesc(profesional.getId(), pacienteId)
                .stream().map(this::toResponse).toList();
    }

    public AnalisisDetalleResponse obtener(Long id, Profesional profesional) {
        return toDetalleResponse(findOwned(id, profesional));
    }

    @Transactional
    public AnalisisResponse crear(String nombre, String imagenTipo, byte[] imagenBytes,
                                  Double escala, Long pacienteId, Profesional profesional) {
        Paciente paciente = resolverPaciente(pacienteId, profesional);
        Analisis a = Analisis.builder()
                .profesional(profesional)
                .paciente(paciente)
                .nombre(nombre)
                .imagenTipo(imagenTipo)
                .imagen(imagenBytes)
                .trazos(new ArrayList<>())
                .escala(escala != null ? escala : 1.0)
                .build();
        return toResponse(analisisRepository.save(a));
    }

    @Transactional
    public AnalisisDetalleResponse actualizar(Long id, AnalisisUpdateRequest req, Profesional profesional) {
        Analisis a = findOwned(id, profesional);
        a.setTrazos(req.trazos());
        a.setEscala(req.escala());
        if (req.pacienteId() != null) {
            a.setPaciente(resolverPaciente(req.pacienteId(), profesional));
        }
        return toDetalleResponse(analisisRepository.save(a));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        analisisRepository.delete(findOwned(id, profesional));
    }

    private Paciente resolverPaciente(Long pacienteId, Profesional profesional) {
        if (pacienteId == null) return null;
        return pacienteRepository.findByIdAndProfesionalId(pacienteId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
    }

    private Analisis findOwned(Long id, Profesional profesional) {
        return analisisRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Análisis no encontrado"));
    }

    private AnalisisResponse toResponse(Analisis a) {
        return new AnalisisResponse(
                a.getId(), a.getNombre(), a.getImagenTipo(), a.getEscala(),
                a.getPaciente() != null ? a.getPaciente().getId()        : null,
                a.getPaciente() != null ? a.getPaciente().getApellido()  : null,
                a.getPaciente() != null ? a.getPaciente().getNombre()    : null,
                a.getTrazos().size(),
                a.getDateCreated(), a.getLastUpdated()
        );
    }

    private AnalisisDetalleResponse toDetalleResponse(Analisis a) {
        return new AnalisisDetalleResponse(
                a.getId(), a.getNombre(), a.getImagenTipo(),
                Base64.getEncoder().encodeToString(a.getImagen()),
                a.getEscala(),
                a.getPaciente() != null ? a.getPaciente().getId()        : null,
                a.getPaciente() != null ? a.getPaciente().getApellido()  : null,
                a.getPaciente() != null ? a.getPaciente().getNombre()    : null,
                a.getTrazos(),
                a.getDateCreated(), a.getLastUpdated()
        );
    }
}
