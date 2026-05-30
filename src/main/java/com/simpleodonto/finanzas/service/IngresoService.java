package com.simpleodonto.finanzas.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.finanzas.domain.Egreso;
import com.simpleodonto.finanzas.domain.EstadoIngreso;
import com.simpleodonto.finanzas.domain.Ingreso;
import com.simpleodonto.finanzas.domain.MedioPago;
import com.simpleodonto.finanzas.dto.FinanzasResumenResponse;
import com.simpleodonto.finanzas.dto.IngresoLibreRequest;
import com.simpleodonto.finanzas.dto.IngresoResponse;
import com.simpleodonto.finanzas.dto.MovimientoResponse;
import com.simpleodonto.finanzas.repository.EgresoRepository;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.finanzas.repository.MedioPagoRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngresoService {

    private final IngresoRepository     ingresoRepository;
    private final EgresoRepository      egresoRepository;
    private final MedioPagoRepository   medioPagoRepository;
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
        Consultorio consultorio = consultorioRepository.findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
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

        // Variación vs mes anterior
        YearMonth ymAnterior = ym.minusMonths(1);
        List<Ingreso> ingresosAnt = ingresoRepository.findByProfesionalIdAndMes(
                profesional.getId(), ymAnterior.atDay(1), ym.atDay(1));
        BigDecimal confirmadoAnt = ingresosAnt.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Double variacionPct = null;
        if (confirmadoAnt.compareTo(BigDecimal.ZERO) > 0) {
            variacionPct = confirmado.subtract(confirmadoAnt)
                    .divide(confirmadoAnt, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Ticket promedio (solo ingresos vinculados a consultas confirmadas)
        List<Ingreso> consultasConfirmadas = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO && i.getConsulta() != null)
                .toList();
        BigDecimal ticketPromedio = null;
        if (!consultasConfirmadas.isEmpty()) {
            BigDecimal totalConsultas = consultasConfirmadas.stream()
                    .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ticketPromedio = totalConsultas.divide(
                    BigDecimal.valueOf(consultasConfirmadas.size()), 0, RoundingMode.HALF_UP);
        }

        return new FinanzasResumenResponse(
                ym.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                confirmado.add(pendiente),
                confirmado,
                pendiente,
                ingresos.size(),
                cantConfirmada,
                cantPendiente,
                variacionPct,
                ticketPromedio
        );
    }

    public Page<MovimientoResponse> movimientos(Profesional profesional, YearMonth ym,
                                                String buscar, Long consultorioId, String tipoFiltro, Pageable pageable) {
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.plusMonths(1).atDay(1);
        boolean hayBuscar = buscar != null && !buscar.isBlank();

        List<Ingreso> ingresos = hayBuscar
                ? ingresoRepository.findByProfesionalIdAndMesAndBuscar(profesional.getId(), desde, hasta, buscar)
                : ingresoRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta);

        List<Egreso> egresos = hayBuscar
                ? egresoRepository.findByProfesionalIdAndMesAndBuscar(profesional.getId(), desde, hasta, buscar)
                : egresoRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta);

        if (consultorioId != null) {
            ingresos = ingresos.stream()
                    .filter(i -> i.getConsultorio() != null && i.getConsultorio().getId().equals(consultorioId))
                    .toList();
            egresos = egresos.stream()
                    .filter(e -> e.getConsultorio() != null && e.getConsultorio().getId().equals(consultorioId))
                    .toList();
        }

        List<MovimientoResponse> all = new ArrayList<>();
        for (Ingreso i : ingresos) {
            boolean esPendiente = i.getEstado() == EstadoIngreso.PENDIENTE;
            if ("egreso".equals(tipoFiltro)) continue;
            if ("ingreso".equals(tipoFiltro) && esPendiente) continue;
            if ("pendiente".equals(tipoFiltro) && !esPendiente) continue;
            String desc = i.getConsulta() != null
                    ? "Consulta · " + String.join(", ", java.util.stream.Stream.of(
                            i.getConsulta().getPaciente().getApellido(),
                            i.getConsulta().getPaciente().getNombre())
                        .filter(s -> s != null && !s.isBlank()).toList())
                    : (i.getDescripcion() != null ? i.getDescripcion() : "Ingreso libre");
            String tipo = esPendiente ? "pendiente" : "ingreso";
            all.add(new MovimientoResponse(tipo, i.getFecha(), desc, i.getMonto(),
                    i.getEstado() != null ? i.getEstado().name() : null));
        }
        for (Egreso e : egresos) {
            if ("ingreso".equals(tipoFiltro) || "pendiente".equals(tipoFiltro)) continue;
            all.add(new MovimientoResponse("egreso", e.getFecha(),
                    e.getDescripcion() != null ? e.getDescripcion() : "Sin descripción",
                    e.getMonto(), null));
        }

        all.sort(Comparator.comparing(MovimientoResponse::fecha).reversed());

        int total = all.size();
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), total);
        List<MovimientoResponse> slice = start >= total ? List.of() : all.subList(start, end);
        return new PageImpl<>(slice, pageable, total);
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
