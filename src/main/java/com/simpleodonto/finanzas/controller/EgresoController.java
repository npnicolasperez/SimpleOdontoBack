package com.simpleodonto.finanzas.controller;

import com.simpleodonto.finanzas.dto.EgresoRequest;
import com.simpleodonto.finanzas.dto.EgresoResponse;
import com.simpleodonto.finanzas.service.EgresoService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/finanzas/egresos")
@RequiredArgsConstructor
public class EgresoController {

    private final EgresoService egresoService;
    private final TokenService  tokenService;

    @GetMapping
    public List<EgresoResponse> listar(
            @RequestParam(required = false) String mes,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        YearMonth ym = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return egresoService.listar(profesional, ym);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EgresoResponse crear(
            @Valid @RequestBody EgresoRequest body,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return egresoService.crear(profesional, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        egresoService.eliminar(profesional, id);
    }
}
