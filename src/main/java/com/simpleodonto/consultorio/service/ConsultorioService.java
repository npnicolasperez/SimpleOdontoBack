package com.simpleodonto.consultorio.service;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.dto.ConsultorioRequest;
import com.simpleodonto.consultorio.dto.ConsultorioResponse;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;

    public List<ConsultorioResponse> listar(Profesional profesional) {
        return consultorioRepository.findByProfesionalId(profesional.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConsultorioResponse crear(ConsultorioRequest req, Profesional profesional) {
        Consultorio consultorio = Consultorio.builder()
                .profesional(profesional)
                .nombre(req.nombre())
                .direccion(req.direccion())
                .build();
        return toResponse(consultorioRepository.save(consultorio));
    }

    @Transactional
    public void eliminar(Long id, Profesional profesional) {
        Consultorio consultorio = consultorioRepository.findByIdAndProfesionalId(id, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        consultorioRepository.delete(consultorio);
    }

    private ConsultorioResponse toResponse(Consultorio c) {
        return new ConsultorioResponse(
                c.getId(), c.getNombre(), c.getDireccion(),
                c.getDateCreated(), c.getLastUpdated()
        );
    }
}
