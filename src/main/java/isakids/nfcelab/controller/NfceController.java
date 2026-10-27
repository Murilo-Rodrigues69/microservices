package isakids.nfcelab.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import isakids.nfcelab.dto.CancelarNfceRequest;
import isakids.nfcelab.dto.ConsultarNfceRequest;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.dto.NfceStatusResponse;
import isakids.nfcelab.service.NfceLabService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@Validated
@RequestMapping({ "/api/nfe", "/api/nfce" })
public class NfceController {

    private final NfceLabService service;

    public NfceController(NfceLabService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<NfceStatusResponse> status(@RequestParam(required = false) Long empresaId) throws Exception {
        return ResponseEntity.ok(service.consultarStatus(empresaId));
    }

    @PostMapping("/xml-preview")
    public ResponseEntity<NfceOperationResponse> xmlPreview(@Valid @RequestBody EmitirNfceRequest request) {
        return ResponseEntity.ok(service.gerarXmlPreview(request));
    }

    @PostMapping("/emitir")
    public ResponseEntity<NfceOperationResponse> emitir(@Valid @RequestBody EmitirNfceRequest request) {
        return ResponseEntity.ok(service.emitir(request));
    }

    @PostMapping("/consultar")
    public ResponseEntity<NfceOperationResponse> consultar(@Valid @RequestBody ConsultarNfceRequest request) {
        return ResponseEntity.ok(service.consultar(request));
    }

    @PostMapping("/cancelar")
    public ResponseEntity<NfceOperationResponse> cancelar(@Valid @RequestBody CancelarNfceRequest request) {
        return ResponseEntity.ok(service.cancelar(request));
    }

    @GetMapping("/documentos/{chaveAcesso}/xml")
    public ResponseEntity<ByteArrayResource> downloadXml(
            @PathVariable @Pattern(regexp = "\\d{44}") String chaveAcesso) {
        byte[] xml = service.loadXml(chaveAcesso);
        if (xml == null) {
            throw new ResponseStatusException(NOT_FOUND, "XML nao encontrado para a chave informada.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + chaveAcesso + ".xml\"")
                .body(new ByteArrayResource(xml));
    }

    @GetMapping("/documentos/{chaveAcesso}/pdf")
    public ResponseEntity<ByteArrayResource> downloadPdf(
            @PathVariable @Pattern(regexp = "\\d{44}") String chaveAcesso) {
        byte[] pdf = service.loadPdf(chaveAcesso);
        if (pdf == null) {
            throw new ResponseStatusException(NOT_FOUND, "PDF nao encontrado para a chave informada.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + chaveAcesso + ".pdf\"")
                .body(new ByteArrayResource(pdf));
    }
}
