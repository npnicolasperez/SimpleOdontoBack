package com.simpleodonto.dashboard.controller;

import com.simpleodonto.dashboard.dto.DashboardResponse;
import com.simpleodonto.dashboard.dto.IngresoMensualDto;
import com.simpleodonto.dashboard.dto.ProximoTurnoDto;
import com.simpleodonto.dashboard.service.DashboardService;
import com.simpleodonto.finanzas.domain.EstadoIngreso;
import com.simpleodonto.consulta.domain.Consulta;
import com.simpleodonto.finanzas.domain.Ingreso;
import com.simpleodonto.profesional.domain.Profesional;
import com.simpleodonto.shared.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TokenService     tokenService;
    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse dashboard(
            HttpServletRequest request,
            @RequestParam(required = false) String mes) throws Exception {
        Profesional prof   = tokenService.resolve(request);
        Long        profId = prof.getId();

        LocalDate     hoy       = LocalDate.now();
        LocalDateTime ahora     = LocalDateTime.now();
        LocalDate     inicioMes = hoy.withDayOfMonth(1);
        LocalDate     finMes    = inicioMes.plusMonths(1);
        LocalDateTime inicioHoy    = hoy.atStartOfDay();
        LocalDateTime finHoy       = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioManana = finHoy;
        LocalDateTime finManana    = hoy.plusDays(2).atStartOfDay();

        // Mes seleccionado para stats de consultas (default: mes actual)
        YearMonth ymSel        = (mes != null && !mes.isBlank()) ? YearMonth.parse(mes) : YearMonth.from(hoy);
        LocalDate inicioMesSel = ymSel.atDay(1);
        LocalDate finMesSel    = ymSel.plusMonths(1).atDay(1);

        // Lanzar todas las queries en paralelo
        CompletableFuture<Long>                      fTotal        = dashboardService.fetchPacientesTotal(profId);
        CompletableFuture<Long>                      fNuevos       = dashboardService.fetchPacientesNuevos(profId, inicioMes.atStartOfDay());
        CompletableFuture<Long>                      fNoVolvieron  = dashboardService.fetchPacientesNoVolvieron(profId, hoy.minusDays(90));
        CompletableFuture<Long>                      fPendHoy      = dashboardService.fetchTurnosPendientesHoy(profId, inicioHoy, finHoy);
        CompletableFuture<Long>                      fPendManana   = dashboardService.fetchTurnosPendientesHoy(profId, inicioManana, finManana);
        CompletableFuture<Optional<ProximoTurnoDto>> fProximo      = dashboardService.fetchProximoTurno(profId, ahora);
        CompletableFuture<List<Ingreso>>             fIngresos     = dashboardService.fetchIngresosMes(profId, inicioMesSel, finMesSel);
        CompletableFuture<List<Consulta>>            fConsultas    = dashboardService.fetchConsultasMes(profId, inicioMesSel, finMesSel);
        CompletableFuture<String>                    fObraSocial   = dashboardService.fetchTopObraSocial(profId);

        CompletableFuture.allOf(fTotal, fNuevos, fNoVolvieron, fPendHoy, fPendManana, fProximo, fIngresos, fConsultas, fObraSocial).join();

        // Financiero
        List<Ingreso> ingresos  = fIngresos.get();
        BigDecimal cobrado = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.CONFIRMADO)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendiente = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.PENDIENTE)
                .map(i -> i.getMonto() != null ? i.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cobrosPendientes = ingresos.stream()
                .filter(i -> i.getEstado() == EstadoIngreso.PENDIENTE).count();

        // Productividad (reutiliza el future ya completado)
        List<Consulta> consultas = fConsultas.get();
        String diaMas = null;
        Double promedio = null;
        if (!consultas.isEmpty()) {
            Map<DayOfWeek, Long> porDia = consultas.stream()
                    .filter(c -> c.getFecha() != null)
                    .collect(Collectors.groupingBy(c -> c.getFecha().getDayOfWeek(), Collectors.counting()));
            diaMas = porDia.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> nombreDia(e.getKey())).orElse(null);
            long diasDistintos = consultas.stream()
                    .filter(c -> c.getFecha() != null)
                    .map(c -> c.getFecha()).distinct().count();
            if (diasDistintos > 0) promedio = (double) consultas.size() / diasDistintos;
        }

        // Próximo turno (mapeado dentro del @Transactional del service para evitar LazyInitializationException)
        ProximoTurnoDto proximoDto = fProximo.get().orElse(null);

        return new DashboardResponse(
                fTotal.get(), fNuevos.get(), fNoVolvieron.get(),
                fPendHoy.get(), proximoDto,
                cobrado.add(pendiente), cobrado, pendiente, cobrosPendientes,
                diaMas, fObraSocial.get(), promedio,
                consultas.size(),
                fPendManana.get()
        );
    }

    @GetMapping("/ingresos-anuales")
    public List<IngresoMensualDto> ingresosAnuales(HttpServletRequest request) {
        Profesional prof = tokenService.resolve(request);
        return dashboardService.getIngresosUltimos12Meses(prof.getId());
    }

    private static String nombreDia(DayOfWeek d) {
        return switch (d) {
            case MONDAY    -> "Lunes";
            case TUESDAY   -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY  -> "Jueves";
            case FRIDAY    -> "Viernes";
            case SATURDAY  -> "Sábado";
            case SUNDAY    -> "Domingo";
        };
    }
}
