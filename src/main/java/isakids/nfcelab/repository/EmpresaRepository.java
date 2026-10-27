package isakids.nfcelab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import isakids.nfcelab.entity.EmpresaEntity;

public interface EmpresaRepository extends JpaRepository<EmpresaEntity, Long> {

    Optional<EmpresaEntity> findByIdAndAtivoTrue(Long id);

    Optional<EmpresaEntity> findByCnpj(String cnpj);

    Optional<EmpresaEntity> findByCnpjAndAtivoTrue(String cnpj);

    List<EmpresaEntity> findByAtivoTrueOrderByNomeAsc();
}
