package com.simpleodonto.estudio.controller;

import com.simpleodonto.estudio.dto.EstudioDetalleResponse;
import com.simpleodonto.estudio.dto.EstudioResponse;
import com.simpleodonto.estudio.dto.EstudioUpdateRequest;
import com.simpleodonto.estudio.service.EstudioService;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/estudios")
@RequiredArgsConstructor
public class EstudioController {

    private final EstudioService estudioService;
    private final TokenService   tokenService;

    @GetMapping
    public Page<EstudioResponse> listar(
            @RequestParam(required = false) String buscar,
            @PageableDefault(size = 30, sort = "lastUpdated", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        return estudioService.listar(buscar, pageable, tokenService.resolve(request));
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<EstudioResponse> listarPorPaciente(@PathVariable Long pacienteId, HttpServletRequest request) {
        return estudioService.listarPorPaciente(pacienteId, tokenService.resolve(request));
    }

    @GetMapping("/{id}")
    public EstudioDetalleResponse obtener(@PathVariable Long id, HttpServletRequest request) {
        return estudioService.obtener(id, tokenService.resolve(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EstudioResponse crear(
            @RequestParam("imagen")                                MultipartFile imagen,
            @RequestParam("nombre")                                String nombre,
            @RequestParam(value = "escala", defaultValue = "1.0")  Double escala,
            @RequestParam("pacienteId")                            Long   pacienteId,
            HttpServletRequest request) throws IOException {
        Profesional profesional = tokenService.resolve(request);
        return estudioService.crear(nombre, imagen.getContentType(), imagen.getBytes(),
                escala, pacienteId, profesional);
    }

    @PutMapping("/{id}")
    public EstudioDetalleResponse actualizar(
            @PathVariable Long id,
            @RequestBody  EstudioUpdateRequest req,
            HttpServletRequest request) {
        return estudioService.actualizar(id, req, tokenService.resolve(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, HttpServletRequest request) {
        estudioService.eliminar(id, tokenService.resolve(request));
    }
}
