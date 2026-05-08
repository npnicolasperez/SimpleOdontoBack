package com.simpleodonto.consulta.controller;

import com.simpleodonto.consulta.dto.ConsultaRequest;
import com.simpleodonto.consulta.dto.ConsultaResponse;
import com.simpleodonto.consulta.service.ConsultaService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;
    private final TokenService    tokenService;

    @GetMapping
    public List<ConsultaResponse> listar(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.listarUltimas(profesional);
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<ConsultaResponse> listarPorPaciente(
            @PathVariable Long pacienteId,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.listarPorPaciente(pacienteId, profesional);
    }

    @PostMapping
    public ResponseEntity<ConsultaResponse> crear(
            @Valid @RequestBody ConsultaRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.crear(req, profesional));
    }
}
