package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Comparativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparativaRepository extends JpaRepository<Comparativa, Long> {

    List<Comparativa> findTop50ByOrderByCreadaEnDesc();
}
