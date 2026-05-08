package com.simpleodonto.consulta.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consulta.dto.ConsultaRequest;
import com.simpleodonto.consulta.dto.ConsultaResponse;
import com.simpleodonto.consulta.repository.ConsultaRepository;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository    consultaRepository;
    private final PacienteRepository    pacienteRepository;
    private final ConsultorioRepository consultorioRepository;

    public List<ConsultaResponse> listarPorPaciente(Long pacienteId, Profesional profesional) {
        var sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        return consultaRepository.findByPacienteIdAndProfesionalId(pacienteId, profesional.getId(), sort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConsultaResponse> listarUltimas(Profesional profesional) {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dateCreated"));
        return consultaRepository.findByProfesionalId(profesional.getId(), pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConsultaResponse crear(ConsultaRequest req, Profesional profesional) {
        Paciente paciente = pacienteRepository
                .findByIdAndProfesionalId(req.pacienteId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));

        Consultorio consultorio = null;
        if (req.consultorioId() != null) {
            consultorio = consultorioRepository
                    .findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        }

        Consulta consulta = Consulta.builder()
                .paciente(paciente)
                .profesional(profesional)
                .consultorio(consultorio)
                .motivoConsulta(req.motivoConsulta())
                .practicaRealizada(req.practicaRealizada())
                .monto(req.monto())
                .tipoPago(req.tipoPago())
                .build();

        return toResponse(consultaRepository.save(consulta));
    }

    private ConsultaResponse toResponse(Consulta c) {
        return new ConsultaResponse(
                c.getId(),
                c.getPaciente().getId(),
                c.getPaciente().getApellido(),
                c.getPaciente().getNombre(),
                c.getProfesional().getId(),
                c.getConsultorio() != null ? c.getConsultorio().getId()     : null,
                c.getConsultorio() != null ? c.getConsultorio().getNombre() : null,
                c.getMotivoConsulta(),
                c.getPracticaRealizada(),
                c.getMonto(),
                c.getTipoPago(),
                c.getDateCreated(),
                c.getLastUpdated()
        );
    }
}
