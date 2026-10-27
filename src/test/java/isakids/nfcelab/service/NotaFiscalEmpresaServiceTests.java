package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import isakids.nfcelab.document.NfceDocumentService;
import isakids.nfcelab.dto.EmitirNfceRequest;
import isakids.nfcelab.dto.NfceOperationResponse;
import isakids.nfcelab.dto.NotaFiscalEmpresaResponse;
import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.entity.AppUserEntity;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;
import isakids.nfcelab.entity.UserRole;
import isakids.nfcelab.model.FormaPagamento;
import isakids.nfcelab.model.ProductData;
import isakids.nfcelab.model.SaleData;
import isakids.nfcelab.model.SaleItemData;
import isakids.nfcelab.repository.ClienteEmpresaRepository;
import isakids.nfcelab.repository.NotaFiscalEmpresaRepository;

class NotaFiscalEmpresaServiceTests {

    @Test
    void registrarEmissaoGravaUsuarioAutenticadoNaNota() {
        NotaFiscalEmpresaRepository notaRepository = mock(NotaFiscalEmpresaRepository.class);
        AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
        NotaFiscalEmpresaService service = newService(notaRepository, authenticatedUserService, mock(EmpresaService.class));

        EmpresaEntity empresa = empresa();
        AppUserEntity usuario = usuario("operador.caixa", "Operador Caixa");
        EmitirNfceRequest request = new EmitirNfceRequest();
        request.setEmpresaId(1L);
        request.setNumeroNota("10");
        request.setFormaPagamento(FormaPagamento.PIX);

        NfceOperationResponse response = new NfceOperationResponse();
        response.setChaveAcesso("52260411222333000144550010000000101000000105");
        response.setCStat(100);
        response.setMotivo("Autorizado o uso da NF-e");

        SaleData saleData = new SaleData(
                null,
                "10",
                LocalDateTime.of(2026, 9, 4, 14, 30),
                BigDecimal.ZERO,
                FormaPagamento.PIX,
                "Venda balcao",
                List.of(new SaleItemData(1L, 1, new BigDecimal("19.90"), null, produto())));

        when(notaRepository.findByChaveAcesso("52260411222333000144550010000000101000000105"))
                .thenReturn(Optional.empty());
        when(authenticatedUserService.getCurrentUser()).thenReturn(usuario);

        service.registrarEmissao(empresa, request, saleData, null, response);

        ArgumentCaptor<NotaFiscalEmpresaEntity> notaCaptor = ArgumentCaptor.forClass(NotaFiscalEmpresaEntity.class);
        verify(notaRepository).save(notaCaptor.capture());

        NotaFiscalEmpresaEntity nota = notaCaptor.getValue();
        assertThat(nota.getUsuarioEmissor()).isSameAs(usuario);
        assertThat(nota.getUsuarioEmissorLogin()).isEqualTo("operador.caixa");
        assertThat(nota.getUsuarioEmissorNome()).isEqualTo("Operador Caixa");
    }

    @Test
    void listarRetornaUsuarioEmissorDaNota() {
        NotaFiscalEmpresaRepository notaRepository = mock(NotaFiscalEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        NotaFiscalEmpresaService service = newService(notaRepository, mock(AuthenticatedUserService.class), empresaService);

        NotaFiscalEmpresaEntity nota = new NotaFiscalEmpresaEntity();
        nota.setEmpresa(empresa());
        nota.setModelo(FiscalModelo.NFCE);
        nota.setChaveAcesso("52260411222333000144550010000000101000000105");
        nota.setUsuarioEmissor(usuario("operador.caixa", "Operador Caixa"));
        nota.setUsuarioEmissorLogin("operador.caixa");
        nota.setUsuarioEmissorNome("Operador Caixa");

        when(notaRepository.findPageByEmpresaId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(nota), PageRequest.of(0, 20), 1));

        PageResponse<NotaFiscalEmpresaResponse> response = service.listar(1L, 0, 20);

        verify(empresaService).requireEmpresa(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).usuarioEmissorLogin()).isEqualTo("operador.caixa");
        assertThat(response.items().get(0).usuarioEmissorNome()).isEqualTo("Operador Caixa");
    }

    private NotaFiscalEmpresaService newService(NotaFiscalEmpresaRepository notaRepository,
            AuthenticatedUserService authenticatedUserService, EmpresaService empresaService) {
        return new NotaFiscalEmpresaService(
                notaRepository,
                mock(ClienteEmpresaRepository.class),
                empresaService,
                new DocumentoFiscalXmlParser(),
                mock(NfceDocumentService.class),
                authenticatedUserService);
    }

    private EmpresaEntity empresa() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(1L);
        empresa.setNome("Isakids");
        empresa.setCnpj("11222333000144");
        empresa.setSerie("1");
        empresa.setModelo(FiscalModelo.NFCE);
        return empresa;
    }

    private AppUserEntity usuario(String login, String name) {
        AppUserEntity usuario = new AppUserEntity();
        usuario.setLogin(login);
        usuario.setName(name);
        usuario.setRole(UserRole.USER);
        usuario.setActive(true);
        usuario.setPasswordHash("hash");
        return usuario;
    }

    private ProductData produto() {
        return new ProductData(1L, "Produto", null, "61091000", null, "0", "5102", "UN");
    }
}
