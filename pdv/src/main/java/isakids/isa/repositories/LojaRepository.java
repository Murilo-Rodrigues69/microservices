package isakids.isa.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import isakids.isa.entities.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {

    List<Loja> findByAtivoTrue();

    Optional<Loja> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
