package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import isakids.nfcelab.dto.ClienteEmpresaResponse;
import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.entity.ClienteEmpresaEntity;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.repository.ClienteEmpresaRepository;

class ClienteEmpresaServiceTests {

    @Test
    void listarAplicaFiltroPorNomeNormalizado() {
        ClienteEmpresaRepository repository = mock(ClienteEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ClienteEmpresaService service = new ClienteEmpresaService(repository, empresaService);

        when(repository.findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAscIdAsc(1L, "MURILO"))
                .thenReturn(List.of(cliente(10L, 1L, "MURILO")));

        List<ClienteEmpresaResponse> response = service.listar(1L, "  MURILO  ");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().nome()).isEqualTo("MURILO");
        verify(empresaService).requireEmpresa(1L);
        verify(repository).findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAscIdAsc(1L, "MURILO");
    }

    @Test
    void listarSemFiltroUsaConsultaSemParametroDeBusca() {
        ClienteEmpresaRepository repository = mock(ClienteEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ClienteEmpresaService service = new ClienteEmpresaService(repository, empresaService);

        when(repository.findByEmpresaIdOrderByNomeAscIdAsc(1L)).thenReturn(List.of(cliente(10L, 1L, "ANA")));

        List<ClienteEmpresaResponse> response = service.listar(1L, null);

        assertThat(response).hasSize(1);
        verify(repository).findByEmpresaIdOrderByNomeAscIdAsc(1L);
        verify(repository, never()).findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAscIdAsc(
                eq(1L), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void listarPaginadoRetornaMetadadosEPassaFiltro() {
        ClienteEmpresaRepository repository = mock(ClienteEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ClienteEmpresaService service = new ClienteEmpresaService(repository, empresaService);

        Page<ClienteEmpresaEntity> page = new PageImpl<>(
                List.of(cliente(20L, 1L, "ANA")),
                PageRequest.of(0, 20),
                1);
        when(repository.findByEmpresaIdAndNomeContainingIgnoreCase(
                eq(1L), eq("ANA"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ClienteEmpresaResponse> response = service.listarPaginado(1L, null, null, " ANA ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByEmpresaIdAndNomeContainingIgnoreCase(eq(1L), eq("ANA"), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.items()).hasSize(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalItems()).isEqualTo(1);
    }

    private ClienteEmpresaEntity cliente(Long id, Long empresaId, String nome) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(empresaId);

        ClienteEmpresaEntity cliente = new ClienteEmpresaEntity();
        cliente.setId(id);
        cliente.setEmpresa(empresa);
        cliente.setNome(nome);
        cliente.setCpfCnpj("12345678901");
        cliente.setCep("75460000");
        cliente.setAtivo(true);
        return cliente;
    }
}
