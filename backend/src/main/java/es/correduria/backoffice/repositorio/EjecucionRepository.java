package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Ejecucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EjecucionRepository extends JpaRepository<Ejecucion, Long> {

    List<Ejecucion> findByInicioAfter(LocalDateTime desde);

    Page<Ejecucion> findAllByOrderByInicioDesc(Pageable pageable);

    Page<Ejecucion> findByAutomatizacionOrderByInicioDesc(String automatizacion, Pageable pageable);

    boolean existsByDemoTrue();

    long deleteByDemoTrue();
}
