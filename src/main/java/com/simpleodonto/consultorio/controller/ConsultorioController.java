package com.simpleodonto.consultorio.controller;

import com.simpleodonto.consultorio.dto.ConsultorioRequest;
import com.simpleodonto.consultorio.dto.ConsultorioResponse;
import com.simpleodonto.consultorio.service.ConsultorioService;
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
@RequestMapping("/api/consultorios")
@RequiredArgsConstructor
public class ConsultorioController {

    private final ConsultorioService consultorioService;
    private final TokenService       tokenService;

    @GetMapping
    public List<ConsultorioResponse> listar(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultorioService.listar(profesional);
    }

    @PostMapping
    public ResponseEntity<ConsultorioResponse> crear(
            @Valid @RequestBody ConsultorioRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(consultorioService.crear(req, profesional));
    }

    @PutMapping("/{id}")
    public ConsultorioResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ConsultorioRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return consultorioService.actualizar(id, req, profesional);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        consultorioService.eliminar(id, profesional);
        return ResponseEntity.noContent().build();
    }
}
