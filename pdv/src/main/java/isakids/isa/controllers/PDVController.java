package isakids.isa.controllers;


import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;
import isakids.isa.services.AuthenticatedUserService;
import isakids.isa.services.VendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


@RestController
@Validated
@RequestMapping("/api/pdv")
public class PDVController {

    @Autowired
    private VendaService vendaService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    public record AbrirVendaRequest(@Positive Long usuarioId) { }
    public record AdicionarItemRequest(@NotBlank String codigoBarras, @NotNull @Positive Integer quantidade) { }
    public record FinalizarVendaRequest(
            @NotNull FormaPagamento formaPagamento,
            @DecimalMin(value = "0.00", inclusive = true) BigDecimal desconto,
            String cpfCnpjConsumidor,
            Boolean incluirCpfNaNota) { }
    public record DescontoRequest(@NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal desconto) { }
    public record EmitirNfceVendaRequest(String cpfCnpjConsumidor, Boolean incluirCpfNaNota) { }

    @PostMapping("/vendas/abrir")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda abrirVenda(@Valid @RequestBody(required = false) AbrirVendaRequest request) {
        Long usuarioId = request != null ? request.usuarioId() : null;
        Usuario usuario = authenticatedUserService.resolveUsuarioOperador(usuarioId);
        return vendaService.abrirVenda(usuario);
    }

    @PostMapping("/vendas/{vendaId}/itens")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda adicionarItem(@PathVariable Long vendaId,
                               @Valid @RequestBody AdicionarItemRequest request) {
        return vendaService.adicionarItem(vendaId, request.codigoBarras(), request.quantidade());
    }

    @DeleteMapping("/vendas/{vendaId}/itens/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda removerItem(
            @PathVariable Long vendaId,
            @PathVariable Long itemId
    ) {
        return vendaService.removerItem(vendaId, itemId);
    }

    @PostMapping("/vendas/{vendaId}/desconto")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda aplicarDesconto(@PathVariable Long vendaId,
                                 @Valid @RequestBody DescontoRequest request) {
        return vendaService.aplicarDesconto(vendaId, request.desconto());
    }

    @PostMapping("/vendas/{vendaId}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda finalizar(@PathVariable Long vendaId,
                           @Valid @RequestBody FinalizarVendaRequest request) {
        return vendaService.finalizarVenda(
                vendaId,
                request.formaPagamento(),
                request.desconto(),
                request.cpfCnpjConsumidor()
        );
    }

    @PostMapping("/vendas/{vendaId}/emitir-nfce")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda emitirNfce(@PathVariable Long vendaId,
                            @Valid @RequestBody(required = false) EmitirNfceVendaRequest request) {
        return vendaService.emitirNfce(
                vendaId,
                request != null ? request.cpfCnpjConsumidor() : null,
                request != null ? request.incluirCpfNaNota() : null);
    }

    @PostMapping("/vendas/{vendaId}/finalizar-e-emitir-nfce")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Venda finalizarEEmitirNfce(@PathVariable Long vendaId,
                                      @Valid @RequestBody FinalizarVendaRequest request) {
        vendaService.finalizarVenda(
                vendaId,
                request.formaPagamento(),
                request.desconto(),
                request.cpfCnpjConsumidor()
        );
        return vendaService.emitirNfce(
                vendaId,
                request.cpfCnpjConsumidor(),
                request.incluirCpfNaNota());
    }

    @PostMapping("/vendas/{vendaId}/cancelar")
    @PreAuthorize("hasRole('USER')")
    public Venda cancelar(@PathVariable Long vendaId,
                          @RequestParam(required = false) String motivo) {
        return vendaService.cancelarVenda(vendaId, motivo);
    }

    @GetMapping("/vendas/{vendaId}")
    @PreAuthorize("hasRole('USER')")
    public Venda buscarVenda(@PathVariable Long vendaId) {
        return vendaService.buscarVendaPdv(vendaId);
    }

    @GetMapping("/vendas/{vendaId}/nfce/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ByteArrayResource> baixarPdfNfce(@PathVariable Long vendaId) {
        Venda venda = vendaService.buscarPorId(vendaId);
        byte[] pdf = vendaService.carregarPdfNfce(vendaId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resolveNfceFileName(venda, "pdf") + "\"")
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/vendas/{vendaId}/nfce/pdf-termico")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ByteArrayResource> baixarPdfNfceTermico(@PathVariable Long vendaId) {
        Venda venda = vendaService.buscarPorId(vendaId);
        byte[] pdf = vendaService.carregarPdfNfceTermico(vendaId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resolveNfceFileName(venda, "termico.pdf") + "\"")
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/vendas/{vendaId}/nfce/xml")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ByteArrayResource> baixarXmlNfce(@PathVariable Long vendaId) {
        Venda venda = vendaService.buscarPorId(vendaId);
        byte[] xml = vendaService.carregarXmlNfce(vendaId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resolveNfceFileName(venda, "xml") + "\"")
                .body(new ByteArrayResource(xml));
    }

    private String resolveNfceFileName(Venda venda, String extensao) {
        if (StringUtils.hasText(venda.getNotaFiscalChaveAcesso())) {
            return venda.getNotaFiscalChaveAcesso() + "." + extensao;
        }
        return "nfce-venda-" + venda.getId() + "." + extensao;
    }
}
