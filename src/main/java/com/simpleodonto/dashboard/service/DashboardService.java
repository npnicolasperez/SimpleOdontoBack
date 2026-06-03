package com.simpleodonto.dashboard.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consulta.repository.ConsultaRepository;
import com.simpleodonto.finanzas.domain.Ingreso;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.dashboard.dto.ProximoTurnoDto;
import com.simpleodonto.turno.domain.EstadoTurno;
import com.simpleodonto.turno.domain.Turno;
import com.simpleodonto.turno.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PacienteRepository  pacienteRepository;
    private final TurnoRepository     turnoRepository;
    private final IngresoRepository   ingresoRepository;
    private final ConsultaRepository  consultaRepository;

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> fetchPacientesTotal(Long profId) {
        return CompletableFuture.completedFuture(
                pacienteRepository.countByProfesionalId(profId));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> fetchPacientesNuevos(Long profId, LocalDateTime inicioMes) {
        return CompletableFuture.completedFuture(
                pacienteRepository.countNuevosDesde(profId, inicioMes));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> fetchPacientesNoVolvieron(Long profId, LocalDate fechaCorte) {
        return CompletableFuture.completedFuture(
                pacienteRepository.countPacientesNoVolvieronDesde(profId, fechaCorte));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> fetchTurnosPendientesHoy(Long profId, LocalDateTime inicio, LocalDateTime fin) {
        return CompletableFuture.completedFuture(
                turnoRepository.countByProfesionalIdAndFechaHoraBetweenAndEstado(profId, inicio, fin, EstadoTurno.PENDIENTE));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<ProximoTurnoDto>> fetchProximoTurno(Long profId, LocalDateTime ahora) {
        Optional<ProximoTurnoDto> dto = turnoRepository
                .findFirstByProfesionalIdAndFechaHoraAfterOrderByFechaHora(profId, ahora)
                .map(t -> {
                    String nombre   = t.getPaciente() != null ? t.getPaciente().getNombre()   : t.getNombrePacienteLibre();
                    String apellido = t.getPaciente() != null ? t.getPaciente().getApellido() : null;
                    return new ProximoTurnoDto(t.getFechaHora(), nombre, apellido);
                });
        return CompletableFuture.completedFuture(dto);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Ingreso>> fetchIngresosMes(Long profId, LocalDate desde, LocalDate hasta) {
        return CompletableFuture.completedFuture(
                ingresoRepository.findByProfesionalIdAndMes(profId, desde, hasta));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Consulta>> fetchConsultasMes(Long profId, LocalDate desde, LocalDate hasta) {
        return CompletableFuture.completedFuture(
                consultaRepository.findByProfesionalIdAndFechaBetween(profId, desde, hasta));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<String> fetchTopObraSocial(Long profId) {
        List<String> top = pacienteRepository.findTopObraSociales(profId, PageRequest.of(0, 1));
        return CompletableFuture.completedFuture(top.isEmpty() ? null : top.get(0));
    }
}
