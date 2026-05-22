package com.simpleodonto.finanzas.controller;

import com.simpleodonto.finanzas.dto.FinanzasResumenResponse;
import com.simpleodonto.finanzas.dto.IngresoLibreRequest;
import com.simpleodonto.finanzas.dto.IngresoResponse;
import com.simpleodonto.finanzas.service.IngresoService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final IngresoService ingresoService;
    private final TokenService   tokenService;

    @GetMapping("/resumen")
    public FinanzasResumenResponse resumen(
            @RequestParam(required = false) String mes,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        YearMonth ym = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return ingresoService.resumenMes(profesional, ym);
    }

    @GetMapping("/ingresos")
    public List<IngresoResponse> ingresos(
            @RequestParam(required = false) String mes,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        YearMonth ym = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return ingresoService.listar(profesional, ym);
    }

    @PostMapping("/ingresos")
    public IngresoResponse crearIngresoLibre(
            @RequestBody IngresoLibreRequest body,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ingresoService.crearLibre(profesional, body);
    }
}
