package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Poliza;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PolizaRepository extends JpaRepository<Poliza, Long>, JpaSpecificationExecutor<Poliza> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "aseguradora"})
    Page<Poliza> findAll(Specification<Poliza> spec, Pageable pageable);

    Optional<Poliza> findByNumeroIgnoreCase(String numero);

    List<Poliza> findByMatriculaIgnoreCase(String matricula);

    List<Poliza> findByClienteIdOrderByFechaVencimientoDesc(Long clienteId);

    long countByEstado(Poliza.Estado estado);

    @Query("select coalesce(sum(p.primaAnual), 0) from Poliza p where p.estado = :estado")
    BigDecimal sumaPrimas(Poliza.Estado estado);

    @Query("select p.ramo, count(p), sum(p.primaAnual) from Poliza p where p.estado = :estado group by p.ramo")
    List<Object[]> carteraPorRamo(Poliza.Estado estado);

    List<Poliza> findTop8ByEstadoAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
            Poliza.Estado estado, LocalDate desde, LocalDate hasta);

    long countByEstadoAndFechaVencimientoBetween(Poliza.Estado estado, LocalDate desde, LocalDate hasta);
}
