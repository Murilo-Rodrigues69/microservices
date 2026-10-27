package isakids.nfcelab.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.entity.SituacaoNotaFiscal;

public record NotaFiscalEmpresaResponse(
        Long id,
        Long empresaId,
        Long clienteEmpresaId,
        Long usuarioEmissorId,
        String usuarioEmissorLogin,
        String usuarioEmissorNome,
        FiscalModelo modelo,
        SituacaoNotaFiscal situacao,
        String numeroNota,
        String serie,
        String chaveAcesso,
        String protocoloAutorizacao,
        Integer cStat,
        String motivo,
        String observacao,
        String qrCodeUrl,
        String destinatarioNome,
        String destinatarioDocumento,
        BigDecimal valorTotal,
        LocalDateTime dataEmissao,
        String xmlDownloadUrl,
        String pdfDownloadUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
