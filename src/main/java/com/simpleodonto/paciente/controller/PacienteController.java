package com.simpleodonto.paciente.controller;

import com.simpleodonto.paciente.dto.OdontogramaRequest;
import com.simpleodonto.paciente.dto.OdontogramaResponse;
import com.simpleodonto.paciente.dto.PacienteRequest;
import com.simpleodonto.paciente.dto.PacienteResponse;
import com.simpleodonto.paciente.dto.PacienteStatsResponse;
import com.simpleodonto.paciente.service.PacienteService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;
    private final TokenService    tokenService;

    @GetMapping("/stats")
    public PacienteStatsResponse stats(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return pacienteService.getStats(profesional);
    }

    @GetMapping
    public Page<PacienteResponse> listar(
            @RequestParam(required = false) String buscar,
            @PageableDefault(size = 20, sort = "apellido", direction = Sort.Direction.ASC) Pageable pageable,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return pacienteService.listar(buscar, pageable, profesional);
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> crear(
            @Valid @RequestBody PacienteRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.crear(req, profesional));
    }

    @GetMapping("/{id}")
    public PacienteResponse obtener(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return pacienteService.obtener(id, profesional);
    }

    @PutMapping("/{id}")
    public PacienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return pacienteService.actualizar(id, req, profesional);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        pacienteService.eliminar(id, profesional);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/odontograma")
    public OdontogramaResponse obtenerOdontograma(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return pacienteService.obtenerOdontograma(id, profesional);
    }

    @PostMapping("/{id}/odontograma")
    public OdontogramaResponse guardarOdontograma(
            @PathVariable Long id,
            @RequestBody OdontogramaRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return pacienteService.guardarOdontograma(id, req, profesional);
    }
}
