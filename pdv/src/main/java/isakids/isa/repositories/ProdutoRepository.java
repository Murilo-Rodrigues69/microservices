package isakids.isa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import isakids.isa.entities.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrue();

    Optional<Produto> findByCodigoBarrasAndAtivoTrue(String codigoBarras);

    List<Produto> findByDescricaoContainingIgnoreCaseAndAtivoTrueOrderByDescricaoAsc(
            String descricao,
            Pageable pageable
    );

    boolean existsByCodigoInterno(String codigoInterno);

    boolean existsByCodigoBarras(String codigoBarras);
}
