package isakids.nfcelab.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoEmpresaResponse(
        Long id,
        Long empresaId,
        String descricao,
        String codigoBarras,
        String ncm,
        String cest,
        String origem,
        String cfop,
        String unidadeComercial,
        BigDecimal precoUnitarioSugerido,
        boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
