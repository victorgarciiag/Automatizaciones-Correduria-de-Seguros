package es.correduria.backoffice.repositorio;

import es.correduria.backoffice.dominio.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByNifIgnoreCase(String nif);

    List<Cliente> findByTelefono(String telefono);

    List<Cliente> findByEmailIgnoreCase(String email);
}
