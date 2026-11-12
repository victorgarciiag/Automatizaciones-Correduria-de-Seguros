package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Aseguradora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AseguradoraRepository extends JpaRepository<Aseguradora, Long> {

    Optional<Aseguradora> findByNombreIgnoreCase(String nombre);
}
