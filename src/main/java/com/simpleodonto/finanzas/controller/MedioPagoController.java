package com.simpleodonto.finanzas.controller;

import com.simpleodonto.finanzas.dto.MedioPagoRequest;
import com.simpleodonto.finanzas.dto.MedioPagoResponse;
import com.simpleodonto.finanzas.service.MedioPagoService;
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
@RequestMapping("/api/medios-pago")
@RequiredArgsConstructor
public class MedioPagoController {

    private final MedioPagoService medioPagoService;
    private final TokenService     tokenService;

    @GetMapping
    public List<MedioPagoResponse> listar(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return medioPagoService.listar(profesional);
    }

    @PostMapping
    public ResponseEntity<MedioPagoResponse> crear(
            @Valid @RequestBody MedioPagoRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(medioPagoService.crear(req, profesional));
    }

    @PutMapping("/{id}")
    public MedioPagoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MedioPagoRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return medioPagoService.actualizar(id, req, profesional);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        medioPagoService.eliminar(id, profesional);
        return ResponseEntity.noContent().build();
    }
}
