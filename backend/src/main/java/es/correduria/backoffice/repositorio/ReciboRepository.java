package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Recibo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ReciboRepository extends JpaRepository<Recibo, Long>, JpaSpecificationExecutor<Recibo> {

    @Override
    @EntityGraph(attributePaths = {"poliza", "poliza.cliente", "poliza.aseguradora"})
    Page<Recibo> findAll(Specification<Recibo> spec, Pageable pageable);

    List<Recibo> findByPolizaIdOrderByFechaEmisionDesc(Long polizaId);

    @EntityGraph(attributePaths = {"poliza", "poliza.cliente", "poliza.aseguradora"})
    List<Recibo> findByEstadoOrderByFechaEmisionAsc(Recibo.Estado estado);

    long countByEstado(Recibo.Estado estado);

    @Query("select coalesce(sum(r.importe), 0) from Recibo r where r.estado = :estado")
    BigDecimal sumaImportes(Recibo.Estado estado);
}
