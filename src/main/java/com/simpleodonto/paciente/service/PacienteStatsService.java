package com.simpleodonto.paciente.service;

import com.simpleodonto.paciente.repository.PacienteRepository;
import com.simpleodonto.turno.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PacienteStatsService {

    private final PacienteRepository pacienteRepository;
    private final TurnoRepository    turnoRepository;

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> contarTotal(Long profesionalId) {
        return CompletableFuture.completedFuture(
                pacienteRepository.countByProfesionalId(profesionalId));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> contarNuevosEsteMes(Long profesionalId) {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return CompletableFuture.completedFuture(
                pacienteRepository.countNuevosDesde(profesionalId, inicioMes));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> contarConTurnoProximo(Long profesionalId) {
        return CompletableFuture.completedFuture(
                turnoRepository.countPacientesConTurnoProximo(
                        profesionalId, LocalDateTime.now()));
    }
}
