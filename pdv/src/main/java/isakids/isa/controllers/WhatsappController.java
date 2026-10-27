package isakids.isa.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.dto.WhatsappSendNfceRequest;
import isakids.isa.dto.WhatsappSendNfceResponse;
import isakids.isa.services.WhatsappService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    @Autowired
    private WhatsappService whatsappService;

    @PostMapping("/vendas/{vendaId}/nfce")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public WhatsappSendNfceResponse enviarNfce(@PathVariable Long vendaId,
            @Valid @RequestBody WhatsappSendNfceRequest request) {
        return whatsappService.enviarNfce(vendaId, request.telefone(), request.legenda());
    }

    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificarWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return ResponseEntity.ok(whatsappService.validarWebhook(mode, verifyToken, challenge));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receberWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) String payload) {
        return ResponseEntity.ok(whatsappService.processarWebhook(signature, payload));
    }
}
