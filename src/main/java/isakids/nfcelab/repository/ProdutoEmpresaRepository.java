package isakids.nfcelab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import isakids.nfcelab.entity.ProdutoEmpresaEntity;

public interface ProdutoEmpresaRepository extends JpaRepository<ProdutoEmpresaEntity, Long> {

    List<ProdutoEmpresaEntity> findByEmpresaIdOrderByDescricaoAscIdAsc(Long empresaId);

    @Query("""
            select produto
            from ProdutoEmpresaEntity produto
            where produto.empresa.id = :empresaId
              and lower(produto.descricao) like lower(concat('%', cast(:descricao as string), '%'))
            order by produto.descricao asc, produto.id asc
            """)
    List<ProdutoEmpresaEntity> findByEmpresaIdAndDescricaoContainingIgnoreCaseOrderByDescricaoAscIdAsc(
            @Param("empresaId") Long empresaId,
            @Param("descricao") String descricao);

    Page<ProdutoEmpresaEntity> findByEmpresaId(Long empresaId, Pageable pageable);

    @Query("""
            select produto
            from ProdutoEmpresaEntity produto
            where produto.empresa.id = :empresaId
              and lower(produto.descricao) like lower(concat('%', cast(:descricao as string), '%'))
            """)
    Page<ProdutoEmpresaEntity> findByEmpresaIdAndDescricaoContainingIgnoreCase(
            @Param("empresaId") Long empresaId,
            @Param("descricao") String descricao,
            Pageable pageable);

    Optional<ProdutoEmpresaEntity> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<ProdutoEmpresaEntity> findByIdAndEmpresaIdAndAtivoTrue(Long id, Long empresaId);
}
