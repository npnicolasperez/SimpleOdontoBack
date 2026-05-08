package com.simpleodonto.profesional.controller;

import com.simpleodonto.profesional.dto.EspecialidadResponse;
import com.simpleodonto.profesional.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadRepository especialidadRepository;

    @GetMapping
    public List<EspecialidadResponse> listar() {
        return especialidadRepository.findAll().stream()
                .map(e -> new EspecialidadResponse(e.getId(), e.getNombre()))
                .toList();
    }
}
