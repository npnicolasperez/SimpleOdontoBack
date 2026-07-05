package com.simpleodonto.obrasocial.controller;

import com.simpleodonto.obrasocial.dto.ObraSocialRequest;
import com.simpleodonto.obrasocial.dto.ObraSocialResponse;
import com.simpleodonto.obrasocial.service.ObraSocialService;
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
@RequestMapping("/api/obras-sociales")
@RequiredArgsConstructor
public class ObraSocialController {

    private final ObraSocialService obraSocialService;
    private final TokenService      tokenService;

    @GetMapping
    public List<ObraSocialResponse> listar(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return obraSocialService.listar(profesional);
    }

    @PostMapping
    public ResponseEntity<ObraSocialResponse> crear(
            @Valid @RequestBody ObraSocialRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(obraSocialService.crear(req, profesional));
    }

    @PutMapping("/{id}")
    public ObraSocialResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ObraSocialRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return obraSocialService.actualizar(id, req, profesional);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        obraSocialService.eliminar(id, profesional);
        return ResponseEntity.noContent().build();
    }
}
