package com.simpleodonto.analisis.controller;

import com.simpleodonto.analisis.dto.AnalisisDetalleResponse;
import com.simpleodonto.analisis.dto.AnalisisResponse;
import com.simpleodonto.analisis.dto.AnalisisUpdateRequest;
import com.simpleodonto.analisis.service.AnalisisService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/analisis")
@RequiredArgsConstructor
public class AnalisisController {

    private final AnalisisService analisisService;
    private final TokenService    tokenService;

    @GetMapping
    public List<AnalisisResponse> listar(HttpServletRequest request) {
        return analisisService.listar(tokenService.resolve(request));
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<AnalisisResponse> listarPorPaciente(@PathVariable Long pacienteId, HttpServletRequest request) {
        return analisisService.listarPorPaciente(pacienteId, tokenService.resolve(request));
    }

    @GetMapping("/{id}")
    public AnalisisDetalleResponse obtener(@PathVariable Long id, HttpServletRequest request) {
        return analisisService.obtener(id, tokenService.resolve(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalisisResponse crear(
            @RequestParam("imagen")                          MultipartFile imagen,
            @RequestParam("nombre")                          String nombre,
            @RequestParam(value = "escala",     defaultValue = "1.0") Double escala,
            @RequestParam(value = "pacienteId", required = false)     Long   pacienteId,
            HttpServletRequest request) throws IOException {
        Profesional profesional = tokenService.resolve(request);
        return analisisService.crear(nombre, imagen.getContentType(), imagen.getBytes(),
                escala, pacienteId, profesional);
    }

    @PutMapping("/{id}")
    public AnalisisDetalleResponse actualizar(
            @PathVariable Long id,
            @RequestBody  AnalisisUpdateRequest req,
            HttpServletRequest request) {
        return analisisService.actualizar(id, req, tokenService.resolve(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, HttpServletRequest request) {
        analisisService.eliminar(id, tokenService.resolve(request));
    }
}
