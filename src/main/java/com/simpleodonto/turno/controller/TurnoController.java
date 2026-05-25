package com.simpleodonto.turno.controller;

import com.simpleodonto.shared.security.TokenService;
import com.simpleodonto.turno.domain.EstadoTurno;
import com.simpleodonto.turno.dto.TurnoRequest;
import com.simpleodonto.turno.dto.TurnoResponse;
import com.simpleodonto.turno.service.TurnoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService  turnoService;
    private final TokenService  tokenService;

    @GetMapping
    public List<TurnoResponse> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            HttpServletRequest request) {
        return turnoService.listar(tokenService.resolve(request), desde, hasta);
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<TurnoResponse> listarPorPaciente(@PathVariable Long pacienteId, HttpServletRequest request) {
        return turnoService.listarPorPaciente(pacienteId, tokenService.resolve(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse crear(@Valid @RequestBody TurnoRequest req, HttpServletRequest request) {
        return turnoService.crear(req, tokenService.resolve(request));
    }

    @PutMapping("/{id}")
    public TurnoResponse actualizar(@PathVariable Long id, @Valid @RequestBody TurnoRequest req, HttpServletRequest request) {
        return turnoService.actualizar(id, req, tokenService.resolve(request));
    }

    @PatchMapping("/{id}/estado")
    public TurnoResponse cambiarEstado(@PathVariable Long id, @RequestParam EstadoTurno estado, HttpServletRequest request) {
        return turnoService.cambiarEstado(id, estado, tokenService.resolve(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, HttpServletRequest request) {
        turnoService.eliminar(id, tokenService.resolve(request));
    }
}
