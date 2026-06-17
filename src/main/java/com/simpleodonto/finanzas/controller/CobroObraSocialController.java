package com.simpleodonto.finanzas.controller;

import com.simpleodonto.finanzas.dto.CobroObraSocialDetalleResponse;
import com.simpleodonto.finanzas.dto.CobroObraSocialRequest;
import com.simpleodonto.finanzas.dto.CobroObraSocialResponse;
import com.simpleodonto.finanzas.dto.IngresoPendientePorOsResponse;
import com.simpleodonto.finanzas.service.CobroObraSocialService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class CobroObraSocialController {

    private final CobroObraSocialService cobroService;
    private final TokenService           tokenService;

    /** Lista los ingresos pendientes de una OS para un consultorio — paso 2 del flow de registrar cobro. */
    @GetMapping("/finanzas/ingresos/pendientes-por-os")
    public List<IngresoPendientePorOsResponse> listarPendientes(
            @RequestParam Long obraSocialId,
            @RequestParam Long consultorioId,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return cobroService.listarPendientesPorOs(obraSocialId, consultorioId, profesional);
    }

    /** Lista los ingresos pendientes con tipoPago=PARTICULAR — para la pestaña Particular del registro de cobros. */
    @GetMapping("/finanzas/ingresos/pendientes-particulares")
    public List<IngresoPendientePorOsResponse> listarPendientesParticulares(HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return cobroService.listarPendientesParticulares(profesional);
    }

    @PostMapping("/cobros-os")
    @ResponseStatus(HttpStatus.CREATED)
    public CobroObraSocialResponse crear(
            @Valid @RequestBody CobroObraSocialRequest req,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return cobroService.crear(req, profesional);
    }

    @GetMapping("/cobros-os")
    public List<CobroObraSocialResponse> listar(
            @RequestParam(required = false) String mes,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        YearMonth ym = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return cobroService.listar(profesional, ym);
    }

    @GetMapping("/cobros-os/{id}")
    public CobroObraSocialDetalleResponse obtener(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        return cobroService.obtener(id, profesional);
    }

    @DeleteMapping("/cobros-os/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id,
            HttpServletRequest request) {
        Profesional profesional = tokenService.resolve(request);
        cobroService.eliminar(id, profesional);
    }
}
