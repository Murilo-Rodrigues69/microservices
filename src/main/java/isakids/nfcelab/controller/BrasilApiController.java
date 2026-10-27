package isakids.nfcelab.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.nfcelab.dto.EmpresaCnpjLookupResponse;
import isakids.nfcelab.service.BrasilApiCnpjService;

@RestController
@RequestMapping("/api/integracoes/brasilapi")
public class BrasilApiController {

    private final BrasilApiCnpjService brasilApiCnpjService;

    public BrasilApiController(BrasilApiCnpjService brasilApiCnpjService) {
        this.brasilApiCnpjService = brasilApiCnpjService;
    }

    @GetMapping("/cnpj")
    public ResponseEntity<EmpresaCnpjLookupResponse> consultarCnpj(@RequestParam String cnpj) {
        return ResponseEntity.ok(brasilApiCnpjService.consultarPorCnpj(cnpj));
    }
}
