package com.simpleodonto.finanzas.controller;

import com.simpleodonto.finanzas.dto.FinanzasResumenResponse;
import com.simpleodonto.finanzas.dto.IngresoLibreRequest;
import com.simpleodonto.finanzas.dto.IngresoResponse;
import com.simpleodonto.finanzas.dto.MovimientoResponse;
import com.simpleodonto.finanzas.service.IngresoService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/movimientos")
    public Page<MovimientoResponse> movimientos(
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long consultorioId,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 30) Pageable pageable,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        YearMonth ym = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return ingresoService.movimientos(profesional, ym, buscar, consultorioId, tipo, pageable);
    }

    @PostMapping("/ingresos")
    public IngresoResponse crearIngresoLibre(
            @Valid @RequestBody IngresoLibreRequest body,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return ingresoService.crearLibre(profesional, body);
    }

    @DeleteMapping("/ingresos/{id}")
    public void eliminarIngreso(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        ingresoService.eliminarLibre(id, profesional);
    }
}
