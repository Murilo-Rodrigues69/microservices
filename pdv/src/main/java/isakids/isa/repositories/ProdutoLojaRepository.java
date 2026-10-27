package isakids.isa.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import isakids.isa.entities.ProdutoLoja;

public interface ProdutoLojaRepository extends JpaRepository<ProdutoLoja, Long> {

    Optional<ProdutoLoja> findByProdutoIdAndLojaId(Long produtoId, Long lojaId);

    Optional<ProdutoLoja> findByProdutoCodigoBarrasAndLojaIdAndProdutoAtivoTrue(String codigoBarras, Long lojaId);

    List<ProdutoLoja> findByLojaIdAndProdutoAtivoTrue(Long lojaId);

    @Query("""
            SELECT pl
            FROM ProdutoLoja pl
            WHERE pl.loja.id = :lojaId
              AND pl.produto.ativo = true
              AND LOWER(pl.produto.descricao) LIKE LOWER(CONCAT('%', :descricao, '%'))
            ORDER BY pl.produto.descricao ASC
            """)
    List<ProdutoLoja> buscarPorDescricaoNaLoja(
            @Param("lojaId") Long lojaId,
            @Param("descricao") String descricao,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(pl.quantidadeEstoque), 0)
            FROM ProdutoLoja pl
            WHERE pl.loja.id = :lojaId
              AND pl.produto.ativo = true
            """)
    Long sumQuantidadeEstoqueByLojaId(@Param("lojaId") Long lojaId);

    @Query("""
            SELECT COALESCE(COUNT(pl), 0)
            FROM ProdutoLoja pl
            WHERE pl.loja.id = :lojaId
              AND pl.produto.ativo = true
              AND pl.quantidadeEstoque <= 5
            """)
    Long countProdutosEstoqueBaixoByLojaId(@Param("lojaId") Long lojaId);

    @Query("""
            SELECT COALESCE(SUM(pl.quantidadeEstoque * pl.produto.precoCusto), 0)
            FROM ProdutoLoja pl
            WHERE pl.loja.id = :lojaId
              AND pl.produto.ativo = true
            """)
    BigDecimal sumValorTotalEstoqueByLojaId(@Param("lojaId") Long lojaId);
}
