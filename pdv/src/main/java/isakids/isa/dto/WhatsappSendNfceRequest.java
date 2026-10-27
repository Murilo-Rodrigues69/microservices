package isakids.isa.dto;

import jakarta.validation.constraints.NotBlank;

public record WhatsappSendNfceRequest(
        @NotBlank String telefone,
        String legenda) {
}
