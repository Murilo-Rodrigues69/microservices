package isakids.nfcelab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import isakids.nfcelab.entity.ClienteEmpresaEntity;

public interface ClienteEmpresaRepository extends JpaRepository<ClienteEmpresaEntity, Long> {

    List<ClienteEmpresaEntity> findByEmpresaIdOrderByNomeAscIdAsc(Long empresaId);

    @Query("""
            select cliente
            from ClienteEmpresaEntity cliente
            where cliente.empresa.id = :empresaId
              and lower(cliente.nome) like lower(concat('%', cast(:nome as string), '%'))
            order by cliente.nome asc, cliente.id asc
            """)
    List<ClienteEmpresaEntity> findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAscIdAsc(
            @Param("empresaId") Long empresaId,
            @Param("nome") String nome);

    Page<ClienteEmpresaEntity> findByEmpresaId(Long empresaId, Pageable pageable);

    @Query("""
            select cliente
            from ClienteEmpresaEntity cliente
            where cliente.empresa.id = :empresaId
              and lower(cliente.nome) like lower(concat('%', cast(:nome as string), '%'))
            """)
    Page<ClienteEmpresaEntity> findByEmpresaIdAndNomeContainingIgnoreCase(
            @Param("empresaId") Long empresaId,
            @Param("nome") String nome,
            Pageable pageable);

    Optional<ClienteEmpresaEntity> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<ClienteEmpresaEntity> findByIdAndEmpresaIdAndAtivoTrue(Long id, Long empresaId);

    Optional<ClienteEmpresaEntity> findByEmpresaIdAndCpfCnpj(Long empresaId, String cpfCnpj);
}
