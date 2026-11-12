package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Siniestro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SiniestroRepository extends JpaRepository<Siniestro, Long> {

    List<Siniestro> findByPolizaIdOrderByFechaAperturaDesc(Long polizaId);

    long countByEstadoIn(Collection<Siniestro.Estado> estados);

    @Query("""
            select s from Siniestro s join fetch s.poliza p join fetch p.cliente
            where s.estado in :activos or s.fechaApertura >= :cerradosDesde
            order by s.fechaApertura desc
            """)
    List<Siniestro> tablero(Collection<Siniestro.Estado> activos, LocalDateTime cerradosDesde);

    @Query("select max(s.id) from Siniestro s")
    Long maxId();
}
