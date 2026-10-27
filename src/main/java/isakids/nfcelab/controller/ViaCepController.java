package isakids.nfcelab.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.nfcelab.dto.EnderecoCepLookupResponse;
import isakids.nfcelab.service.ViaCepService;

@RestController
@RequestMapping("/api/integracoes/viacep")
public class ViaCepController {

    private final ViaCepService viaCepService;

    public ViaCepController(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
    }

    @GetMapping("/cep")
    public ResponseEntity<EnderecoCepLookupResponse> consultarCep(@RequestParam String cep) {
        return ResponseEntity.ok(viaCepService.consultarPorCep(cep));
    }
}
