package es.correduria.backoffice.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> contenido, long total, int pagina, int tamano) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapper) {
        return new Pagina<>(page.map(mapper).getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
