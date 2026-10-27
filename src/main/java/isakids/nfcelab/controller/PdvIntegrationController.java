package isakids.nfcelab.controller;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import isakids.nfcelab.config.PdvIntegrationProperties;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;
import isakids.nfcelab.dto.PdvNfceIntegrationRequest;
import isakids.nfcelab.service.PdvIntegrationService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/integracoes/pdv")
public class PdvIntegrationController {

    private final PdvIntegrationService pdvIntegrationService;
    private final PdvIntegrationProperties properties;

    public PdvIntegrationController(PdvIntegrationService pdvIntegrationService, PdvIntegrationProperties properties) {
        this.pdvIntegrationService = pdvIntegrationService;
        this.properties = properties;
    }

    @PostMapping("/nfce")
    public ResponseEntity<NfceOperationResponse> emitirNfce(
            @RequestHeader(name = "X-Integration-Token", required = false) String integrationToken,
            @Valid @RequestBody PdvNfceIntegrationRequest request) {
        validateToken(integrationToken);
        return ResponseEntity.ok(pdvIntegrationService.emitirNfce(request));
    }

    @GetMapping("/nfce/vendas/{vendaId}/pdf")
    public ResponseEntity<ByteArrayResource> baixarPdf(
            @RequestHeader(name = "X-Integration-Token", required = false) String integrationToken,
            @PathVariable Long vendaId,
            @RequestParam String lojaCnpj) {
        validateToken(integrationToken);
        NotaFiscalEmpresaEntity nota = pdvIntegrationService.buscarNotaFiscal(lojaCnpj, vendaId);
        byte[] pdf = pdvIntegrationService.loadPdf(lojaCnpj, vendaId);
        if (pdf == null || pdf.length == 0) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "PDF da NFC-e nao encontrado para a venda informada.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + nota.getChaveAcesso() + ".pdf\"")
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/nfce/vendas/{vendaId}/xml")
    public ResponseEntity<ByteArrayResource> baixarXml(
            @RequestHeader(name = "X-Integration-Token", required = false) String integrationToken,
            @PathVariable Long vendaId,
            @RequestParam String lojaCnpj) {
        validateToken(integrationToken);
        NotaFiscalEmpresaEntity nota = pdvIntegrationService.buscarNotaFiscal(lojaCnpj, vendaId);
        byte[] xml = pdvIntegrationService.loadXml(lojaCnpj, vendaId);
        if (xml == null || xml.length == 0) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "XML da NFC-e nao encontrado para a venda informada.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + nota.getChaveAcesso() + ".xml\"")
                .body(new ByteArrayResource(xml));
    }

    private void validateToken(String integrationToken) {
        if (!StringUtils.hasText(properties.getToken())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Integracao PDV desabilitada: configure APP_INTEGRACOES_PDV_TOKEN.");
        }
        if (!properties.getToken().trim().equals(integrationToken)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Token de integracao invalido.");
        }
    }
}
