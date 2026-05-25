package com.simpleodonto.finanzas.service;

import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.finanzas.domain.Egreso;
import com.simpleodonto.finanzas.dto.EgresoRequest;
import com.simpleodonto.finanzas.dto.EgresoResponse;
import com.simpleodonto.finanzas.repository.EgresoRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EgresoService {

    private final EgresoRepository      egresoRepository;
    private final ConsultorioRepository consultorioRepository;

    public List<EgresoResponse> listar(Profesional profesional, YearMonth ym) {
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.plusMonths(1).atDay(1);
        return egresoRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EgresoResponse crear(Profesional profesional, EgresoRequest req) {
        Consultorio consultorio = consultorioRepository.findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        Egreso egreso = Egreso.builder()
                .fecha(req.fecha() != null ? req.fecha() : LocalDate.now())
                .monto(req.monto())
                .descripcion(req.descripcion())
                .profesional(profesional)
                .consultorio(consultorio)
                .build();
        return toResponse(egresoRepository.save(egreso));
    }

    @Transactional
    public void eliminar(Profesional profesional, Long id) {
        Egreso egreso = egresoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Egreso no encontrado"));
        if (!egreso.getProfesional().getId().equals(profesional.getId()))
            throw new IllegalArgumentException("No autorizado");
        egresoRepository.delete(egreso);
    }

    private EgresoResponse toResponse(Egreso e) {
        return new EgresoResponse(
                e.getId(),
                e.getFecha(),
                e.getMonto(),
                e.getDescripcion(),
                e.getConsultorio().getId(),
                e.getConsultorio().getNombre(),
                e.getDateCreated()
        );
    }
}
