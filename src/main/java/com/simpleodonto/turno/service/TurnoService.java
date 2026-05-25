package com.simpleodonto.turno.service;

import com.simpleodonto.calendario.service.GoogleCalendarService;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.paciente.domain.Paciente;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.turno.domain.EstadoTurno;
import com.simpleodonto.turno.domain.Turno;
import com.simpleodonto.turno.dto.TurnoRequest;
import com.simpleodonto.turno.dto.TurnoResponse;
import com.simpleodonto.turno.repository.TurnoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository        turnoRepository;
    private final PacienteRepository     pacienteRepository;
    private final ConsultorioRepository  consultorioRepository;
    private final GoogleCalendarService  googleCalendarService;

    public List<TurnoResponse> listar(Profesional profesional, LocalDateTime desde, LocalDateTime hasta) {
        return turnoRepository
                .findByProfesionalIdAndFechaHoraBetweenOrderByFechaHora(profesional.getId(), desde, hasta)
                .stream().map(this::toResponse).toList();
    }

    public List<TurnoResponse> listarPorPaciente(Long pacienteId, Profesional profesional) {
        return turnoRepository
                .findByProfesionalIdAndPacienteIdOrderByFechaHoraDesc(profesional.getId(), pacienteId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TurnoResponse crear(TurnoRequest req, Profesional profesional) {
        Turno turno = Turno.builder()
                .profesional(profesional)
                .paciente(resolverPaciente(req.pacienteId(), profesional))
                .nombrePacienteLibre(req.nombrePacienteLibre())
                .consultorio(resolverConsultorio(req.consultorioId(), profesional))
                .fechaHora(req.fechaHora())
                .duracionMinutos(req.duracionMinutos() != null ? req.duracionMinutos() : 30)
                .motivo(req.motivo())
                .estado(req.estado() != null ? req.estado() : EstadoTurno.PENDIENTE)
                .build();

        turno = turnoRepository.save(turno);

        String googleEventId = googleCalendarService.crearEvento(profesional, turno);
        if (googleEventId != null) {
            turno.setGoogleEventId(googleEventId);
            turno = turnoRepository.save(turno);
        }

        return toResponse(turno);
    }

    @Transactional
    public TurnoResponse actualizar(Long id, TurnoRequest req, Profesional profesional) {
        Turno turno = findOwned(id, profesional);

        turno.setPaciente(resolverPaciente(req.pacienteId(), profesional));
        turno.setNombrePacienteLibre(req.nombrePacienteLibre());
        turno.setConsultorio(resolverConsultorio(req.consultorioId(), profesional));
        turno.setFechaHora(req.fechaHora());
        if (req.duracionMinutos() != null) turno.setDuracionMinutos(req.duracionMinutos());
        turno.setMotivo(req.motivo());
        if (req.estado() != null) turno.setEstado(req.estado());

        turno = turnoRepository.save(turno);
        googleCalendarService.actualizarEvento(profesional, turno);

        return toResponse(turno);
    }

    @Transactional
    public TurnoResponse cambiarEstado(Long id, EstadoTurno estado, Profesional profesional) {
        Turno turno = findOwned(id, profesional);
        turno.setEstado(estado);
        turno = turnoRepository.save(turno);
        googleCalendarService.actualizarEvento(profesional, turno);
        return toResponse(turno);
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        Turno turno = findOwned(id, profesional);
        googleCalendarService.eliminarEvento(profesional, turno.getGoogleEventId());
        turnoRepository.delete(turno);
    }

    private Turno findOwned(Long id, Profesional profesional) {
        return turnoRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado"));
    }

    private Paciente resolverPaciente(Long id, Profesional profesional) {
        if (id == null) return null;
        return pacienteRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
    }

    private Consultorio resolverConsultorio(Long id, Profesional profesional) {
        if (id == null) return null;
        return consultorioRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
    }

    private TurnoResponse toResponse(Turno t) {
        return new TurnoResponse(
                t.getId(),
                t.getPaciente() != null ? t.getPaciente().getId()        : null,
                t.getPaciente() != null ? t.getPaciente().getNombre()    : null,
                t.getPaciente() != null ? t.getPaciente().getApellido()  : null,
                t.getNombrePacienteLibre(),
                t.getConsultorio() != null ? t.getConsultorio().getId()     : null,
                t.getConsultorio() != null ? t.getConsultorio().getNombre() : null,
                t.getFechaHora(), t.getDuracionMinutos(), t.getMotivo(),
                t.getEstado(), t.getGoogleEventId(),
                t.getDateCreated(), t.getLastUpdated()
        );
    }
}
