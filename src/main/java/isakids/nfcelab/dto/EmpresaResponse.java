package isakids.nfcelab.dto;

import java.time.LocalDateTime;

import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;

public record EmpresaResponse(
        Long id,
        String nome,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String inscricaoEstadual,
        String serie,
        String naturezaOperacao,
        String cep,
        String logradouro,
        String numero,
        String bairro,
        String codigoMunicipio,
        String descricaoMunicipio,
        String uf,
        FiscalModelo modelo,
        FiscalAmbiente ambiente,
        String certificadoPfxPath,
        String cadeiaJksPath,
        Integer cscId,
        boolean ativo,
        boolean certificadoSenhaConfigurada,
        boolean cadeiaSenhaConfigurada,
        boolean cscConfigurado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
