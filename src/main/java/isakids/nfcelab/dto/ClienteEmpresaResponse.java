package isakids.nfcelab.dto;

import java.time.LocalDateTime;

public record ClienteEmpresaResponse(
        Long id,
        Long empresaId,
        String nome,
        String cpfCnpj,
        String inscricaoEstadual,
        String cep,
        String logradouro,
        String numero,
        String bairro,
        String codigoMunicipio,
        String descricaoMunicipio,
        String uf,
        String email,
        boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
