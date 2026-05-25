package com.simpleodonto.paciente.repository;

import com.simpleodonto.paciente.domain.Odontograma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdontogramaRepository extends JpaRepository<Odontograma, Long> {
    Optional<Odontograma> findFirstByPacienteIdOrderByLastUpdatedDesc(Long pacienteId);
    List<Odontograma>     findByPacienteIdOrderByDateCreatedDesc(Long pacienteId);
}
