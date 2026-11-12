package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.AltaPoliza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AltaPolizaRepository extends JpaRepository<AltaPoliza, Long> {

    List<AltaPoliza> findTop50ByOrderByCreadaEnDesc();

    List<AltaPoliza> findByEstado(AltaPoliza.Estado estado);

    List<AltaPoliza> findByEstadoAndPolizaNumeroIgnoreCase(AltaPoliza.Estado estado, String polizaNumero);
}
