package isakids.nfcelab.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;

public interface NotaFiscalEmpresaRepository extends JpaRepository<NotaFiscalEmpresaEntity, Long> {

    Optional<NotaFiscalEmpresaEntity> findByChaveAcesso(String chaveAcesso);

    @Query("""
            select n
            from NotaFiscalEmpresaEntity n
            join fetch n.empresa
            where n.chaveAcesso = :chaveAcesso
            """)
    Optional<NotaFiscalEmpresaEntity> findByChaveAcessoWithEmpresa(@Param("chaveAcesso") String chaveAcesso);

    Optional<NotaFiscalEmpresaEntity> findByEmpresaIdAndOrigemSistemaAndVendaOrigemId(
            Long empresaId,
            String origemSistema,
            Long vendaOrigemId);

    @Query(
            value = """
                    select n
                    from NotaFiscalEmpresaEntity n
                    join fetch n.empresa
                    left join fetch n.usuarioEmissor
                    where n.empresa.id = :empresaId
                    order by coalesce(n.dataEmissao, n.createdAt) desc, n.id desc
                    """,
            countQuery = """
                    select count(n)
                    from NotaFiscalEmpresaEntity n
                    where n.empresa.id = :empresaId
                    """)
    Page<NotaFiscalEmpresaEntity> findPageByEmpresaId(@Param("empresaId") Long empresaId, Pageable pageable);
}
