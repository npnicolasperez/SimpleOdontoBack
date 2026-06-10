package com.simpleodonto.paciente.migration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migración one-shot al boot: mueve los datos de obra social desde las columnas legacy de {@code paciente}
 * (obra_social_id, nro_afiliado, plan_obra_social, titular_obra_social) a la nueva tabla {@code paciente_obra_social},
 * y backfillea {@code consulta.obra_social_id} / {@code ingreso.obra_social_id} para registros con
 * {@code tipo_pago='OBRA_SOCIAL'} a partir de la obra social del paciente.
 * <p>
 * Es idempotente: si las columnas legacy ya no existen, o si los registros ya fueron migrados,
 * no hace nada. Se puede dejar corriendo en cada boot sin riesgo.
 * <p>
 * Las columnas legacy de {@code paciente} se dejan en la BD (Hibernate ddl-auto=update no las dropea).
 * Cuando confirmemos que todo anda, se dropean con SQL manual.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class ObraSocialMigrationRunner implements ApplicationRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!columnaExiste("paciente", "obra_social_id")) {
            log.info("[ObraSocialMigration] columnas legacy ya no existen — skip");
            return;
        }

        int filasPaciente = (int) em.createNativeQuery("""
            INSERT INTO paciente_obra_social (paciente_id, obra_social_id, nro_afiliado,
                                               plan_obra_social, titular_obra_social, orden,
                                               date_created, last_updated)
            SELECT p.id, p.obra_social_id, p.nro_afiliado,
                   p.plan_obra_social, p.titular_obra_social, 0,
                   COALESCE(p.last_updated, NOW()), COALESCE(p.last_updated, NOW())
            FROM paciente p
            WHERE p.obra_social_id IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM paciente_obra_social pos
                  WHERE pos.paciente_id = p.id AND pos.obra_social_id = p.obra_social_id
              )
            """).executeUpdate();
        log.info("[ObraSocialMigration] {} paciente(s) migrados a paciente_obra_social", filasPaciente);

        int filasConsulta = (int) em.createNativeQuery("""
            UPDATE consulta c
            SET obra_social_id = p.obra_social_id
            FROM paciente p
            WHERE c.paciente_id = p.id
              AND c.tipo_pago = 'OBRA_SOCIAL'
              AND c.obra_social_id IS NULL
              AND p.obra_social_id IS NOT NULL
            """).executeUpdate();
        log.info("[ObraSocialMigration] {} consulta(s) con obra_social_id backfilled", filasConsulta);

        int filasIngreso = (int) em.createNativeQuery("""
            UPDATE ingreso i
            SET obra_social_id = p.obra_social_id
            FROM consulta c
            JOIN paciente p ON p.id = c.paciente_id
            WHERE i.consulta_id = c.id
              AND i.tipo_pago = 'OBRA_SOCIAL'
              AND i.obra_social_id IS NULL
              AND p.obra_social_id IS NOT NULL
            """).executeUpdate();
        log.info("[ObraSocialMigration] {} ingreso(s) de consulta con obra_social_id backfilled", filasIngreso);
    }

    private boolean columnaExiste(String tabla, String columna) {
        Object result = em.createNativeQuery("""
            SELECT 1 FROM information_schema.columns
            WHERE table_name = :tabla AND column_name = :columna
            """)
            .setParameter("tabla", tabla)
            .setParameter("columna", columna)
            .getResultList()
            .stream().findFirst().orElse(null);
        return result != null;
    }
}
