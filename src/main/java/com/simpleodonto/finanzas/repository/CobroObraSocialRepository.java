package com.simpleodonto.finanzas.repository;

import com.simpleodonto.finanzas.domain.CobroObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CobroObraSocialRepository extends JpaRepository<CobroObraSocial, Long> {

    Optional<CobroObraSocial> findByIdAndProfesionalId(Long id, Long profesionalId);

    boolean existsByObraSocialId(Long obraSocialId);

    boolean existsByConsultorioId(Long consultorioId);

    boolean existsByMedioPagoId(Long medioPagoId);

    @Query("""
        SELECT c FROM CobroObraSocial c
        WHERE c.profesional.id = :profId
          AND c.fecha >= :desde AND c.fecha < :hasta
        ORDER BY c.fecha DESC, c.id DESC
        """)
    List<CobroObraSocial> findByProfesionalIdAndMes(@Param("profId") Long profId,
                                                     @Param("desde") LocalDate desde,
                                                     @Param("hasta") LocalDate hasta);
}
