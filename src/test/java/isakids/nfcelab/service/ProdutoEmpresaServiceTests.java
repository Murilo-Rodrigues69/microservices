package isakids.nfcelab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.dto.ProdutoEmpresaResponse;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.ProdutoEmpresaEntity;
import isakids.nfcelab.repository.ProdutoEmpresaRepository;

class ProdutoEmpresaServiceTests {

    @Test
    void listarAplicaFiltroPorNomeNormalizado() {
        ProdutoEmpresaRepository repository = mock(ProdutoEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ProdutoEmpresaService service = new ProdutoEmpresaService(repository, empresaService);

        when(repository.findByEmpresaIdAndDescricaoContainingIgnoreCaseOrderByDescricaoAscIdAsc(1L, "TIJOLO"))
                .thenReturn(List.of(produto(10L, 1L, "TIJOLO 6 FUROS")));

        List<ProdutoEmpresaResponse> response = service.listar(1L, "  TIJOLO  ");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().descricao()).isEqualTo("TIJOLO 6 FUROS");
        verify(empresaService).requireEmpresa(1L);
        verify(repository).findByEmpresaIdAndDescricaoContainingIgnoreCaseOrderByDescricaoAscIdAsc(1L, "TIJOLO");
    }

    @Test
    void listarSemFiltroUsaConsultaSemParametroDeBusca() {
        ProdutoEmpresaRepository repository = mock(ProdutoEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ProdutoEmpresaService service = new ProdutoEmpresaService(repository, empresaService);

        when(repository.findByEmpresaIdOrderByDescricaoAscIdAsc(1L))
                .thenReturn(List.of(produto(10L, 1L, "ARGAMASSA")));

        List<ProdutoEmpresaResponse> response = service.listar(1L, "   ");

        assertThat(response).hasSize(1);
        verify(repository).findByEmpresaIdOrderByDescricaoAscIdAsc(1L);
        verify(repository, never()).findByEmpresaIdAndDescricaoContainingIgnoreCaseOrderByDescricaoAscIdAsc(
                eq(1L), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void listarPaginadoRetornaMetadadosEPassaFiltro() {
        ProdutoEmpresaRepository repository = mock(ProdutoEmpresaRepository.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ProdutoEmpresaService service = new ProdutoEmpresaService(repository, empresaService);

        Page<ProdutoEmpresaEntity> page = new PageImpl<>(
                List.of(produto(20L, 1L, "TELHA COLONIAL")),
                PageRequest.of(0, 20),
                1);
        when(repository.findByEmpresaIdAndDescricaoContainingIgnoreCase(
                eq(1L), eq("TELHA"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProdutoEmpresaResponse> response = service.listarPaginado(1L, null, null, " TELHA ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByEmpresaIdAndDescricaoContainingIgnoreCase(
                eq(1L), eq("TELHA"), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.items()).hasSize(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalItems()).isEqualTo(1);
    }

    private ProdutoEmpresaEntity produto(Long id, Long empresaId, String descricao) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(empresaId);

        ProdutoEmpresaEntity produto = new ProdutoEmpresaEntity();
        produto.setId(id);
        produto.setEmpresa(empresa);
        produto.setDescricao(descricao);
        produto.setPrecoUnitarioSugerido(BigDecimal.valueOf(1.25));
        produto.setAtivo(true);
        return produto;
    }
}
