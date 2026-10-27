package isakids.isa.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import isakids.isa.entities.MovimentacaoEstoque;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque> findByLojaId(Long lojaId);

    List<MovimentacaoEstoque> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<MovimentacaoEstoque> findByLojaIdAndDataHoraBetween(Long lojaId, LocalDateTime inicio, LocalDateTime fim);

    List<MovimentacaoEstoque> findByDataHoraGreaterThanEqual(LocalDateTime inicio);

    List<MovimentacaoEstoque> findByLojaIdAndDataHoraGreaterThanEqual(Long lojaId, LocalDateTime inicio);

    List<MovimentacaoEstoque> findByDataHoraLessThanEqual(LocalDateTime fim);

    List<MovimentacaoEstoque> findByLojaIdAndDataHoraLessThanEqual(Long lojaId, LocalDateTime fim);
}
