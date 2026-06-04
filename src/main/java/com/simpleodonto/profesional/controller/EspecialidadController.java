package com.simpleodonto.profesional.controller;

import com.simpleodonto.profesional.dto.EspecialidadRequest;
import com.simpleodonto.profesional.dto.EspecialidadResponse;
import com.simpleodonto.profesional.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<EspecialidadResponse> listar() {
        return especialidadService.listar();
    }

    @PostMapping
    public ResponseEntity<EspecialidadResponse> crear(@Valid @RequestBody EspecialidadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especialidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
