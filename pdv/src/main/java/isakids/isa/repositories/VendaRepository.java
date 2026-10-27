package isakids.isa.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import isakids.isa.entities.Venda;
import isakids.isa.enums.StatusVenda;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findByLojaId(Long lojaId);

    @Query("""
            select v.id
            from Venda v
            where v.usuario.id = :usuarioId
              and v.loja.id = :lojaId
              and (v.status = :status or v.status is null)
            order by v.dataHora desc
            """)
    List<Long> findIdsByUsuarioIdAndLojaIdAndStatusOrStatusNullOrderByDataHoraDesc(
            @Param("usuarioId") Long usuarioId,
            @Param("lojaId") Long lojaId,
            @Param("status") StatusVenda status);

    @Query("""
            select distinct v
            from Venda v
            left join fetch v.loja
            left join fetch v.usuario
            left join fetch v.itens iv
            left join fetch iv.produto
            where v.id = :id
            """)
    java.util.Optional<Venda> findDetalhadaById(@Param("id") Long id);

    @Query("""
            SELECT v
            FROM Venda v
            WHERE v.dataHora BETWEEN :inicio AND :fim
              AND (:status IS NULL OR v.status = :status)
              AND (:lojaId IS NULL OR v.loja.id = :lojaId)
            ORDER BY v.dataHora DESC
        """)
    List<Venda> buscarPorPeriodoStatusELoja(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusVenda status,
            @Param("lojaId") Long lojaId
    );

    @Query("""
            SELECT
                COALESCE(SUM(v.totalBruto), 0),
                COALESCE(SUM(v.desconto), 0),
                COUNT(v)
            FROM Venda v
            WHERE v.dataHora BETWEEN :inicio AND :fim
              AND (:status IS NULL OR v.status = :status)
              AND (:lojaId IS NULL OR v.loja.id = :lojaId)
        """)
    List<Object[]> resumoSemItens(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusVenda status,
            @Param("lojaId") Long lojaId
    );

    @Query("""
            SELECT
                COALESCE(SUM(iv.quantidade * p.precoCusto), 0)
            FROM Venda v
            JOIN v.itens iv
            JOIN iv.produto p
            WHERE v.dataHora BETWEEN :inicio AND :fim
              AND (:status IS NULL OR v.status = :status)
              AND (:lojaId IS NULL OR v.loja.id = :lojaId)
        """)
    BigDecimal custoTotalPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusVenda status,
            @Param("lojaId") Long lojaId
    );
}
