package com.simpleodonto.finanzas.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.finanzas.domain.EstadoIngreso;
import com.simpleodonto.finanzas.domain.Ingreso;
import com.simpleodonto.finanzas.domain.MedioPago;
import com.simpleodonto.finanzas.dto.FinanzasResumenResponse;
import com.simpleodonto.finanzas.dto.IngresoLibreRequest;
import com.simpleodonto.finanzas.dto.IngresoResponse;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.finanzas.repository.MedioPagoRepository;
import com.simpleodonto.profesional.domain.Profesional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngresoService {

    private final IngresoRepository    ingresoRepository;
    private final MedioPagoRepository  medioPagoRepository;
    private final ConsultorioRepository consultorioRepository;

    @Transactional
    public void crearDesdeConsulta(Consulta consulta, Long medioPagoId) {
        Ingreso ingreso = Ingreso.builder()
                .fecha(consulta.getFecha() != null ? consulta.getFecha() : LocalDate.now())
                .consulta(consulta)
                .profesional(consulta.getProfesional())
                .monto(consulta.getMonto())
                .estado(estadoDesde(consulta.getMonto()))
                .tipoPago(consulta.getTipoPago())
                .medioPago(resolverMedioPago(medioPagoId, consulta.getProfesional()))
                .consultorio(consulta.getConsultorio())
                .build();
        ingresoRepository.save(ingreso);
    }

    @Transactional
    public void actualizarDesdeConsulta(Consulta consulta, Long medioPagoId) {
        ingresoRepository.findByConsultaId(consulta.getId()).ifPresent(ingreso -> {
            ingreso.setFecha(consulta.getFecha() != null ? consulta.getFecha() : LocalDate.now());
            ingreso.setMonto(consulta.getMonto());
            ingreso.setEstado(estadoDesde(consulta.getMonto()));
            ingreso.setTipoPago(consulta.getTipoPago());
            ingreso.setMedioPago(resolverMedioPago(medioPagoId, consulta.getProfesional()));
            ingreso.setConsultorio(consulta.getConsultorio());
            ingresoRepository.save(ingreso);
        });
    }

    @Transactional
    public void eliminarPorConsulta(Long consultaId) {
        ingresoRepository.findByConsultaId(consultaId).ifPresent(ingresoRepository::delete);
    }

    @Transactional
    public IngresoResponse crearLibre(Profesional profesional, IngresoLibreRequest req) {
        if (req.consultorioId() == null) throw new IllegalArgumentException("consultorioId es obligatorio");
        Consultorio consultorio = consultorioRepository.findById(req.consultorioId())
                .orElseThrow(() -> new IllegalArgumentException("Consultorio no encontrado"));
        Ingreso ingreso = Ingreso.builder()
                .fecha(LocalDate.now())
                .consulta(null)
                .profesional(profesional)
                .descripcion(req.descripcion())
                .monto(req.monto())
                .estado(estadoDesde(req.monto()))
                .tipoPago(req.tipoPago())
                .medioPago(resolverMedioPago(req.medioPagoId(), profesional))
                .consultorio(consultorio)
                .build();
        return toResponse(ingresoRepository.save(ingreso));
    }

    public List<IngresoResponse> listar(Profesional profesional, YearMonth ym) {
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.plusMonths(1).atDay(1);
        return ingresoRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta)
                .stream().map(this::toResponse).toList();
    }

    public FinanzasResumenResponse resumenMes(Profesional profesional, YearMonth ym) {
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.plusMonths(1).atDay(1);

        List<Ingreso> ingresos = ingresoRepository.findByProfesionalIdAndMes(
                profesional.getId(), desde, hasta);

        BigDecimal confirmado = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendiente = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.PENDIENTE)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cantConfirmada = ingresos.stream().filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO).count();
        long cantPendiente  = ingresos.stream().filter(i -> i.getEstado() == EstadoIngreso.PENDIENTE).count();

        return new FinanzasResumenResponse(
                ym.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                confirmado.add(pendiente),
                confirmado,
                pendiente,
                ingresos.size(),
                cantConfirmada,
                cantPendiente
        );
    }

    private EstadoIngreso estadoDesde(BigDecimal monto) {
        return (monto != null && monto.compareTo(BigDecimal.ZERO) > 0)
                ? EstadoIngreso.CONFIRMADO : EstadoIngreso.PENDIENTE;
    }

    private MedioPago resolverMedioPago(Long medioPagoId, Profesional profesional) {
        if (medioPagoId == null) return null;
        return medioPagoRepository.findByIdAndProfesionalId(medioPagoId, profesional.getId()).orElse(null);
    }

    private IngresoResponse toResponse(Ingreso i) {
        String pacienteNombre   = i.getConsulta() != null ? i.getConsulta().getPaciente().getNombre()   : null;
        String pacienteApellido = i.getConsulta() != null ? i.getConsulta().getPaciente().getApellido() : null;
        return new IngresoResponse(
                i.getId(),
                i.getConsulta() != null ? i.getConsulta().getId() : null,
                pacienteNombre,
                pacienteApellido,
                i.getDescripcion(),
                i.getMonto(),
                i.getEstado(),
                i.getTipoPago(),
                i.getMedioPago()   != null ? i.getMedioPago().getId()      : null,
                i.getMedioPago()   != null ? i.getMedioPago().getNombre()  : null,
                i.getConsultorio() != null ? i.getConsultorio().getId()    : null,
                i.getConsultorio() != null ? i.getConsultorio().getNombre(): null,
                i.getFecha(),
                i.getDateCreated()
        );
    }
}
