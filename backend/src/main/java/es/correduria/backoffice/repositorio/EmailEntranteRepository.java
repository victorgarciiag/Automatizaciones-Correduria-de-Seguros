package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.EmailEntrante;
import es.correduria.backoffice.dominio.Poliza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailEntranteRepository extends JpaRepository<EmailEntrante, Long> {

    List<EmailEntrante> findTop100ByOrderByCreadoEnDesc();

    /** Quita la referencia a una póliza (p. ej. antes de borrarla al limpiar pruebas): el email se conserva. */
    @Modifying
    @Query("update EmailEntrante e set e.poliza = null where e.poliza = :poliza")
    int desvincularPoliza(@Param("poliza") Poliza poliza);
}
