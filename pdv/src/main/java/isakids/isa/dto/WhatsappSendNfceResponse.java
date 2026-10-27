package isakids.isa.dto;

public record WhatsappSendNfceResponse(
        Long vendaId,
        String telefone,
        String mediaId,
        String messageId,
        String status) {
}
