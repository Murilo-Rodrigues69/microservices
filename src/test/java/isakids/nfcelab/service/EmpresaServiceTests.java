package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import isakids.nfcelab.dto.EmpresaRequest;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.repository.EmpresaRepository;

class EmpresaServiceTests {

    @Test
    void criarEmpresaNfceExigeCsc() {
        EmpresaRepository repository = mock(EmpresaRepository.class);
        EmpresaService service = new EmpresaService(repository);
        EmpresaRequest request = empresaRequest(FiscalModelo.NFCE);
        request.setCsc(null);
        request.setCscId(1);

        when(repository.findByCnpj("11222333000144")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("csc deve ser informado.");
    }

    @Test
    void criarEmpresaNfceExigeCscIdMaiorQueZero() {
        EmpresaRepository repository = mock(EmpresaRepository.class);
        EmpresaService service = new EmpresaService(repository);
        EmpresaRequest request = empresaRequest(FiscalModelo.NFCE);
        request.setCsc("TOKEN-CSC");
        request.setCscId(0);

        when(repository.findByCnpj("11222333000144")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe cscId maior que zero para a empresa NFC-e.");
    }

    @Test
    void criarEmpresaNfceSalvaCscECscId() {
        EmpresaRepository repository = mock(EmpresaRepository.class);
        EmpresaService service = new EmpresaService(repository);
        EmpresaRequest request = empresaRequest(FiscalModelo.NFCE);
        request.setCsc("  TOKEN-CSC  ");
        request.setCscId(2);

        when(repository.findByCnpj("11222333000144")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(EmpresaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.criar(request);

        assertThat(response.modelo()).isEqualTo(FiscalModelo.NFCE);
        assertThat(response.cscId()).isEqualTo(2);
        assertThat(response.cscConfigurado()).isTrue();
    }

    private EmpresaRequest empresaRequest(FiscalModelo modelo) {
        EmpresaRequest request = new EmpresaRequest();
        request.setNome("LOJA DEMONSTRACAO");
        request.setCnpj("11.222.333/0001-44");
        request.setRazaoSocial("LOJA DEMONSTRACAO LTDA");
        request.setNomeFantasia("LOJA DEMONSTRACAO");
        request.setInscricaoEstadual("109876543");
        request.setSerie("1");
        request.setNaturezaOperacao("VENDA DE MERCADORIA");
        request.setCep("75460000");
        request.setLogradouro("AVENIDA TESTE");
        request.setNumero("100");
        request.setBairro("CENTRO");
        request.setCodigoMunicipio("5214507");
        request.setDescricaoMunicipio("NEROPOLIS");
        request.setUf("GO");
        request.setModelo(modelo);
        request.setAmbiente(FiscalAmbiente.PRODUCAO);
        request.setCertificadoPfxPath("C:/certificados/isakids.pfx");
        request.setCertificadoSenha("123456");
        request.setCadeiaJksPath("C:/certificados/sefaz-producao.jks");
        request.setCadeiaSenha("senha");
        request.setAtivo(true);
        return request;
    }
}
