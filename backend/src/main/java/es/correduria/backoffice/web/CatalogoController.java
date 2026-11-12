package es.correduria.backoffice.web;

import es.correduria.backoffice.dominio.Aseguradora;
import es.correduria.backoffice.repositorio.AseguradoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aseguradoras")
@RequiredArgsConstructor
public class CatalogoController {

    private final AseguradoraRepository aseguradoras;

    @GetMapping
    public List<Aseguradora> listar() {
        return aseguradoras.findAll(Sort.by("nombre"));
    }
}
