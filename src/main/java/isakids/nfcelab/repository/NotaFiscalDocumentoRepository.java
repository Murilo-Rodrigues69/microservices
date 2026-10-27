package isakids.nfcelab.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import isakids.nfcelab.entity.NotaFiscalDocumentoEntity;

public interface NotaFiscalDocumentoRepository extends JpaRepository<NotaFiscalDocumentoEntity, Long> {

    Optional<NotaFiscalDocumentoEntity> findByChaveAcesso(String chaveAcesso);
}
