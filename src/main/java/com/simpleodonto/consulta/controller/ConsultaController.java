package com.simpleodonto.consulta.controller;

import com.simpleodonto.consulta.dto.ArchivoInfo;
import com.simpleodonto.consulta.dto.ConsultaRequest;
import com.simpleodonto.consulta.dto.ConsultaResponse;
import com.simpleodonto.consulta.dto.ConsultaUpdateRequest;
import com.simpleodonto.consulta.service.ConsultaService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;
    private final TokenService    tokenService;

    @GetMapping("/paciente/{pacienteId}")
    public List<ConsultaResponse> listarPorPaciente(
            @PathVariable Long pacienteId,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.listarPorPaciente(pacienteId, profesional);
    }

    @GetMapping("/{id}")
    public ConsultaResponse obtener(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.obtener(id, profesional);
    }

    @PostMapping
    public ResponseEntity<ConsultaResponse> crear(
            @Valid @RequestBody ConsultaRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.crear(req, profesional));
    }

    @PutMapping("/{id}")
    public ConsultaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ConsultaUpdateRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.actualizar(id, req, profesional);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        consultaService.eliminar(id, profesional);
    }

    @PostMapping("/{id}/archivos")
    public ArchivoInfo agregarArchivo(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.agregarArchivo(id, archivo, profesional);
    }

    @GetMapping("/{id}/archivos/{archivoId}")
    public ResponseEntity<byte[]> descargarArchivo(
            @PathVariable Long id,
            @PathVariable Long archivoId,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultaService.descargarArchivo(id, archivoId, profesional);
    }

    @DeleteMapping("/{id}/archivos/{archivoId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void eliminarArchivo(
            @PathVariable Long id,
            @PathVariable Long archivoId,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        consultaService.eliminarArchivo(id, archivoId, profesional);
    }
}
