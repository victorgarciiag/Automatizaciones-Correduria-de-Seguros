package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Automatizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomatizacionRepository extends JpaRepository<Automatizacion, String> {

    List<Automatizacion> findAllByOrderByOrdenAsc();
}
