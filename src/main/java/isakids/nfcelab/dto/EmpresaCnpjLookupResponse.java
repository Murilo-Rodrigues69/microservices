package isakids.nfcelab.dto;

import java.math.BigDecimal;

public record EmpresaCnpjLookupResponse(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String situacaoCadastral,
        String descricaoSituacaoCadastral,
        String dataInicioAtividade,
        String naturezaJuridica,
        String porte,
        BigDecimal capitalSocial,
        String email,
        String telefone1,
        String telefone2,
        String cep,
        String tipoLogradouro,
        String logradouro,
        String logradouroCompleto,
        String numero,
        String complemento,
        String bairro,
        String municipio,
        String codigoMunicipioIbge,
        String uf) {
}
