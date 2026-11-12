package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Revision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisionRepository extends JpaRepository<Revision, Long> {

    List<Revision> findByEstadoOrderByCreadaEnDesc(Revision.Estado estado);

    List<Revision> findTop50ByEstadoNotOrderByResueltaEnDesc(Revision.Estado estado);

    long countByEstado(Revision.Estado estado);

    long deleteByDemoTrue();

    long deleteByAutomatizacion(String automatizacion);
}
