package com.simpleodonto.finanzas.service;

import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.consulta.domain.TipoPago;
import com.simpleodonto.consulta.repository.ConsultaRepository;
import com.simpleodonto.consultorio.domain.Consultorio;
import com.simpleodonto.consultorio.repository.ConsultorioRepository;
import com.simpleodonto.finanzas.domain.CobroObraSocial;
import com.simpleodonto.finanzas.domain.Egreso;
import com.simpleodonto.finanzas.domain.EstadoIngreso;
import com.simpleodonto.finanzas.domain.Ingreso;
import com.simpleodonto.finanzas.domain.MedioPago;
import com.simpleodonto.finanzas.dto.EstadisticaAnualDto;
import com.simpleodonto.finanzas.dto.FinanzasResumenResponse;
import com.simpleodonto.finanzas.dto.IngresoLibreRequest;
import com.simpleodonto.finanzas.dto.IngresoResponse;
import com.simpleodonto.finanzas.dto.MovimientoResponse;
import com.simpleodonto.finanzas.repository.CobroObraSocialRepository;
import com.simpleodonto.finanzas.repository.EgresoRepository;
import com.simpleodonto.finanzas.repository.IngresoRepository;
import com.simpleodonto.finanzas.repository.MedioPagoRepository;
import com.simpleodonto.obrasocial.domain.ObraSocial;
import com.simpleodonto.obrasocial.repository.ObraSocialRepository;
import com.simpleodonto.profesional.domain.Profesional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final IngresoRepository         ingresoRepository;
    private final EgresoRepository          egresoRepository;
    private final MedioPagoRepository       medioPagoRepository;
    private final ConsultorioRepository     consultorioRepository;
    private final ConsultaRepository        consultaRepository;
    private final ObraSocialRepository      obraSocialRepository;
    private final CobroObraSocialRepository cobroRepository;

    @Transactional
    public void crearDesdeConsulta(Consulta consulta, Long medioPagoId, Boolean pendienteCobro) {
        Ingreso ingreso = Ingreso.builder()
                .fecha(consulta.getFecha() != null ? consulta.getFecha() : LocalDate.now())
                .consulta(consulta)
                .profesional(consulta.getProfesional())
                .monto(consulta.getMonto())
                .estado(resolverEstado(pendienteCobro, consulta.getMonto()))
                .tipoPago(consulta.getTipoPago())
                .medioPago(resolverMedioPago(medioPagoId, consulta.getProfesional()))
                .obraSocial(consulta.getObraSocial())
                .consultorio(consulta.getConsultorio())
                .build();
        ingresoRepository.save(ingreso);
    }

    @Transactional
    public void actualizarDesdeConsulta(Consulta consulta, Long medioPagoId, Boolean pendienteCobro) {
        ingresoRepository.findByConsultaId(consulta.getId()).ifPresent(ingreso -> {
            boolean estabaEnCobro = ingreso.getCobroObraSocial() != null;
            boolean quierePendiente = Boolean.TRUE.equals(pendienteCobro);

            // ESCAPE HATCH — el ingreso estaba dentro de un cobro batch Y el usuario activó "Dejar cobro
            // pendiente" desde la consulta. La desvinculamos del cobro y la dejamos pendiente nuevamente.
            // El cobro mismo no se toca (su montoRecibido queda como estaba; el usuario puede editarlo o
            // eliminarlo aparte). monto vuelve a lo que dice la consulta (null para OS, número para particular).
            if (estabaEnCobro && quierePendiente) {
                ingreso.setCobroObraSocial(null);
                ingreso.setEstado(EstadoIngreso.PENDIENTE);
                ingreso.setMonto(consulta.getMonto());
                ingreso.setFecha(consulta.getFecha() != null ? consulta.getFecha() : LocalDate.now());
                ingreso.setTipoPago(consulta.getTipoPago());
                ingreso.setMedioPago(resolverMedioPago(medioPagoId, consulta.getProfesional()));
                ingreso.setObraSocial(consulta.getObraSocial());
                ingreso.setConsultorio(consulta.getConsultorio());
                ingresoRepository.save(ingreso);
                return;
            }

            // Si el ingreso ya fue cerrado por un cobro batch de OS y el usuario NO pidió volver a
            // pendiente, ese cobro es la fuente de verdad. Solo refrescamos metadata leve.
            if (estabaEnCobro) {
                ingreso.setObraSocial(consulta.getObraSocial());
                ingreso.setConsultorio(consulta.getConsultorio());
                ingresoRepository.save(ingreso);
                return;
            }

            ingreso.setFecha(consulta.getFecha() != null ? consulta.getFecha() : LocalDate.now());
            ingreso.setMonto(consulta.getMonto());
            ingreso.setEstado(resolverEstado(pendienteCobro, consulta.getMonto()));
            ingreso.setTipoPago(consulta.getTipoPago());
            ingreso.setMedioPago(resolverMedioPago(medioPagoId, consulta.getProfesional()));
            ingreso.setObraSocial(consulta.getObraSocial());
            ingreso.setConsultorio(consulta.getConsultorio());
            ingresoRepository.save(ingreso);
        });
    }

    /**
     * Estado del ingreso para una consulta: si el flag pendienteCobro viene en true → PENDIENTE,
     * en false → CONFIRMADO. Si viene null (cliente legacy), cae al cálculo viejo basado en monto.
     */
    private EstadoIngreso resolverEstado(Boolean pendienteCobro, BigDecimal monto) {
        if (pendienteCobro != null) {
            return pendienteCobro ? EstadoIngreso.PENDIENTE : EstadoIngreso.CONFIRMADO;
        }
        return estadoDesde(monto);
    }

    @Transactional
    public void eliminarPorConsulta(Long consultaId) {
        ingresoRepository.findByConsultaId(consultaId).ifPresent(ingresoRepository::delete);
    }

    /**
     * Elimina un ingreso libre (no vinculado a consulta). Los ingresos de consulta se eliminan
     * borrando la consulta, no desde acá.
     */
    @Transactional
    public void eliminarLibre(Long ingresoId, Profesional profesional) {
        Ingreso ingreso = ingresoRepository.findById(ingresoId)
                .orElseThrow(() -> new EntityNotFoundException("Ingreso no encontrado"));
        if (!ingreso.getProfesional().getId().equals(profesional.getId())) {
            throw new EntityNotFoundException("Ingreso no encontrado");
        }
        if (ingreso.getConsulta() != null) {
            throw new IllegalArgumentException("No se puede eliminar un ingreso vinculado a una consulta. Eliminá la consulta.");
        }
        ingresoRepository.delete(ingreso);
    }

    @Transactional
    public IngresoResponse crearLibre(Profesional profesional, IngresoLibreRequest req) {
        Consultorio consultorio = consultorioRepository.findByIdAndProfesionalId(req.consultorioId(), profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Consultorio no encontrado"));
        ObraSocial obraSocial = resolverObraSocial(req.tipoPago(), req.obraSocialId(), profesional);
        Ingreso ingreso = Ingreso.builder()
                .fecha(LocalDate.now())
                .consulta(null)
                .profesional(profesional)
                .descripcion(req.descripcion())
                .monto(req.monto())
                .estado(estadoDesde(req.monto()))
                .tipoPago(req.tipoPago())
                .medioPago(resolverMedioPago(req.medioPagoId(), profesional))
                .obraSocial(obraSocial)
                .consultorio(consultorio)
                .build();
        return toResponse(ingresoRepository.save(ingreso));
    }

    /**
     * Resuelve la obra social de un ingreso libre. Si tipoPago=OBRA_SOCIAL, obraSocialId es obligatorio.
     */
    private ObraSocial resolverObraSocial(TipoPago tipoPago, Long obraSocialId, Profesional profesional) {
        if (tipoPago != TipoPago.OBRA_SOCIAL) return null;
        if (obraSocialId == null) {
            throw new IllegalArgumentException("Seleccioná una obra social para este ingreso.");
        }
        return obraSocialRepository.findByIdAndProfesionalId(obraSocialId, profesional.getId())
                .orElseThrow(() -> new EntityNotFoundException("Obra social no encontrada"));
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

        // Ingresos del mes excluyendo los cubiertos por un cobro batch — esos se contabilizan
        // a través del cobro (ingreso virtual) para evitar doble conteo.
        List<Ingreso> ingresos = ingresoRepository.findByProfesionalIdAndMes(
                profesional.getId(), desde, hasta).stream()
                .filter(i -> i.getCobroObraSocial() == null)
                .toList();

        List<CobroObraSocial> cobros = cobroRepository.findByProfesionalIdAndMes(
                profesional.getId(), desde, hasta);

        BigDecimal confirmadoIngresos = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal confirmadoCobros = cobros.stream()
                .map(c -> c.getMontoRecibido() != null ? c.getMontoRecibido() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal confirmado = confirmadoIngresos.add(confirmadoCobros);

        BigDecimal pendiente = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.PENDIENTE)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cantConfirmadaIngresos = ingresos.stream().filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO).count();
        long cantConfirmada = cantConfirmadaIngresos + cobros.size();
        long cantPendiente  = ingresos.stream().filter(i -> i.getEstado() == EstadoIngreso.PENDIENTE).count();
        long cantTotal      = cantConfirmada + cantPendiente;

        // Variación vs mes anterior — misma definición (ingresos sin cobro + cobros del mes anterior).
        YearMonth ymAnterior = ym.minusMonths(1);
        LocalDate desdeAnt = ymAnterior.atDay(1);
        LocalDate hastaAnt = ym.atDay(1);
        List<Ingreso> ingresosAnt = ingresoRepository.findByProfesionalIdAndMes(
                profesional.getId(), desdeAnt, hastaAnt).stream()
                .filter(i -> i.getCobroObraSocial() == null)
                .toList();
        List<CobroObraSocial> cobrosAnt = cobroRepository.findByProfesionalIdAndMes(
                profesional.getId(), desdeAnt, hastaAnt);
        BigDecimal confirmadoAnt = ingresosAnt.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(cobrosAnt.stream()
                        .map(c -> c.getMontoRecibido() != null ? c.getMontoRecibido() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        Double variacionPct = null;
        if (confirmadoAnt.compareTo(BigDecimal.ZERO) > 0) {
            variacionPct = confirmado.subtract(confirmadoAnt)
                    .divide(confirmadoAnt, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Ticket promedio — se calcula sobre consultas con monto > 0 (particulares).
        // Para OS no hay monto a nivel consulta, así que no contribuyen al promedio. Es el comportamiento
        // que el profesional quiere: el "ticket" es el de pacientes particulares.
        List<Ingreso> consultasConMonto = ingresos.stream()
                .filter(i -> i.getConsulta() != null && i.getMonto() != null && i.getMonto().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        BigDecimal ticketPromedio = null;
        if (!consultasConMonto.isEmpty()) {
            BigDecimal totalConsultas = consultasConMonto.stream()
                    .map(Ingreso::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ticketPromedio = totalConsultas.divide(
                    BigDecimal.valueOf(consultasConMonto.size()), 0, RoundingMode.HALF_UP);
        }

        return new FinanzasResumenResponse(
                ym.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                confirmado.add(pendiente),
                confirmado,
                pendiente,
                cantTotal,
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

        // Excluimos ingresos cubiertos por cobro: aparecen como una sola fila del cobro más abajo.
        ingresos = ingresos.stream()
                .filter(i -> i.getCobroObraSocial() == null)
                .toList();

        List<Egreso> egresos = hayBuscar
                ? egresoRepository.findByProfesionalIdAndMesAndBuscar(profesional.getId(), desde, hasta, buscar)
                : egresoRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta);

        List<CobroObraSocial> cobros = cobroRepository.findByProfesionalIdAndMes(profesional.getId(), desde, hasta);
        if (hayBuscar) {
            String b = buscar.toLowerCase();
            cobros = cobros.stream()
                    .filter(c -> {
                        String os   = c.getObraSocial() != null ? c.getObraSocial().getNombre() : "";
                        String desc = c.getDescripcion() != null ? c.getDescripcion() : "";
                        return os.toLowerCase().contains(b) || desc.toLowerCase().contains(b);
                    })
                    .toList();
        }

        if (consultorioId != null) {
            ingresos = ingresos.stream()
                    .filter(i -> i.getConsultorio() != null && i.getConsultorio().getId().equals(consultorioId))
                    .toList();
            egresos = egresos.stream()
                    .filter(e -> e.getConsultorio() != null && e.getConsultorio().getId().equals(consultorioId))
                    .toList();
            cobros = cobros.stream()
                    .filter(c -> c.getConsultorio() != null && c.getConsultorio().getId().equals(consultorioId))
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
            String origen = i.getConsulta() != null ? "consulta" : "libre";
            Long consultaId = i.getConsulta() != null ? i.getConsulta().getId() : null;
            Long pacienteId = (i.getConsulta() != null && i.getConsulta().getPaciente() != null) ? i.getConsulta().getPaciente().getId() : null;
            Long   osId     = i.getObraSocial() != null ? i.getObraSocial().getId()     : null;
            String osNombre = i.getObraSocial() != null ? i.getObraSocial().getNombre() : null;
            all.add(new MovimientoResponse(i.getId(), origen, tipo, i.getFecha(), desc, i.getMonto(),
                    i.getEstado() != null ? i.getEstado().name() : null, consultaId, pacienteId, osId, osNombre, null, null));
        }
        // Cobros de OS: cada uno entra como UN movimiento de tipo ingreso. Origen "cobro_os" para que
        // el front sepa que no se elimina desde acá (la eliminación vive en la pestaña Cobros).
        for (CobroObraSocial c : cobros) {
            if ("egreso".equals(tipoFiltro) || "pendiente".equals(tipoFiltro)) continue;
            String osNombre = c.getObraSocial() != null ? c.getObraSocial().getNombre() : "Obra social";
            String desc = "Cobro · " + osNombre;
            Long   osId     = c.getObraSocial() != null ? c.getObraSocial().getId() : null;
            all.add(new MovimientoResponse(c.getId(), "cobro_os", "ingreso", c.getFecha(), desc, c.getMontoRecibido(),
                    EstadoIngreso.CONFIRMADO.name(), null, null, osId, osNombre, c.getId(), c.getFecha()));
        }
        for (Egreso e : egresos) {
            if ("ingreso".equals(tipoFiltro) || "pendiente".equals(tipoFiltro)) continue;
            all.add(new MovimientoResponse(e.getId(), "egreso", "egreso", e.getFecha(),
                    e.getDescripcion() != null ? e.getDescripcion() : "Sin descripción",
                    e.getMonto(), null, null, null, null, null, null, null));
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
                i.getObraSocial()  != null ? i.getObraSocial().getId()     : null,
                i.getObraSocial()  != null ? i.getObraSocial().getNombre() : null,
                i.getConsultorio() != null ? i.getConsultorio().getId()    : null,
                i.getConsultorio() != null ? i.getConsultorio().getNombre(): null,
                i.getFecha(),
                i.getDateCreated()
        );
    }

    /**
     * Estadísticas mensuales para los últimos 12 meses (incluido el mes actual).
     * Cada fila trae: total de ingresos CONFIRMADOS (incluyendo cobros de OS como ingresos virtuales),
     * promedio de monto cobrado por consulta y cantidad de consultas.
     * Los meses sin actividad vienen con valores en 0 (no se omiten) para que el gráfico no tenga huecos.
     */
    @Transactional(readOnly = true)
    public List<EstadisticaAnualDto> getEstadisticasUltimos12Meses(Profesional profesional, Long consultorioId) {
        Long profId = profesional.getId();
        LocalDate hoy   = LocalDate.now();
        LocalDate desde = hoy.withDayOfMonth(1).minusMonths(11);
        LocalDate hasta = hoy.withDayOfMonth(1).plusMonths(1);

        // Ingresos confirmados por mes — excluimos los cubiertos por cobro batch (se cuentan a través del cobro).
        List<Ingreso> ingresos = ingresoRepository.findConfirmadosByProfesionalIdAndRangoAndConsultorio(
                profId, desde, hasta, consultorioId).stream()
                .filter(i -> i.getCobroObraSocial() == null)
                .toList();
        java.util.Map<YearMonth, BigDecimal> ingresosPorMes = new java.util.HashMap<>();
        for (Ingreso i : ingresos) {
            if (i.getFecha() == null) continue;
            YearMonth ym = YearMonth.from(i.getFecha());
            BigDecimal monto = i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO;
            ingresosPorMes.merge(ym, monto, BigDecimal::add);
        }

        // Cobros de OS del rango — sumamos como ingresos virtuales, filtrando por consultorio si aplica.
        List<CobroObraSocial> cobros = cobroRepository.findByProfesionalIdAndMes(profId, desde, hasta);
        if (consultorioId != null) {
            cobros = cobros.stream()
                    .filter(c -> c.getConsultorio() != null && c.getConsultorio().getId().equals(consultorioId))
                    .toList();
        }
        for (CobroObraSocial c : cobros) {
            if (c.getFecha() == null) continue;
            YearMonth ym = YearMonth.from(c.getFecha());
            BigDecimal monto = c.getMontoRecibido() != null ? c.getMontoRecibido() : BigDecimal.ZERO;
            ingresosPorMes.merge(ym, monto, BigDecimal::add);
        }

        // Consultas del rango — para el promedio usamos el monto de la consulta (lo que cobra el profesional).
        // Para OS la consulta no tiene monto, así que ese promedio refleja consultas particulares.
        List<Consulta> consultas = consultaRepository.findByProfesionalIdAndFechaBetweenAndConsultorio(profId, desde, hasta, consultorioId);
        java.util.Map<YearMonth, BigDecimal> sumaMontosPorMes = new java.util.HashMap<>();
        java.util.Map<YearMonth, Long>       cantidadPorMes   = new java.util.HashMap<>();
        for (Consulta c : consultas) {
            if (c.getFecha() == null) continue;
            YearMonth ym = YearMonth.from(c.getFecha());
            BigDecimal monto = c.getMonto() != null ? c.getMonto() : BigDecimal.ZERO;
            sumaMontosPorMes.merge(ym, monto, BigDecimal::add);
            cantidadPorMes.merge(ym, 1L, Long::sum);
        }

        List<EstadisticaAnualDto> resultado = new ArrayList<>(12);
        YearMonth actual = YearMonth.from(desde);
        YearMonth fin    = YearMonth.from(hoy);
        while (!actual.isAfter(fin)) {
            BigDecimal totalIngresos = ingresosPorMes.getOrDefault(actual, BigDecimal.ZERO);
            long cant = cantidadPorMes.getOrDefault(actual, 0L);
            BigDecimal sumaMontos = sumaMontosPorMes.getOrDefault(actual, BigDecimal.ZERO);
            BigDecimal promedio = cant > 0
                    ? sumaMontos.divide(BigDecimal.valueOf(cant), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            resultado.add(new EstadisticaAnualDto(actual.getYear(), actual.getMonthValue(), totalIngresos, promedio, cant));
            actual = actual.plusMonths(1);
        }
        return resultado;
    }
}
