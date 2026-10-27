package isakids.nfcelab.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import isakids.nfcelab.dto.ClienteEmpresaRequest;
import isakids.nfcelab.dto.ClienteEmpresaResponse;
import isakids.nfcelab.dto.EmpresaRequest;
import isakids.nfcelab.dto.EmpresaResponse;
import isakids.nfcelab.dto.NotaFiscalEmpresaResponse;
import isakids.nfcelab.dto.PageResponse;
import isakids.nfcelab.dto.ProdutoEmpresaRequest;
import isakids.nfcelab.dto.ProdutoEmpresaResponse;
import isakids.nfcelab.service.ClienteEmpresaService;
import isakids.nfcelab.service.EmpresaService;
import isakids.nfcelab.service.NotaFiscalEmpresaService;
import isakids.nfcelab.service.ProdutoEmpresaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final ProdutoEmpresaService produtoEmpresaService;
    private final ClienteEmpresaService clienteEmpresaService;
    private final NotaFiscalEmpresaService notaFiscalEmpresaService;

    public EmpresaController(EmpresaService empresaService, ProdutoEmpresaService produtoEmpresaService,
            ClienteEmpresaService clienteEmpresaService, NotaFiscalEmpresaService notaFiscalEmpresaService) {
        this.empresaService = empresaService;
        this.produtoEmpresaService = produtoEmpresaService;
        this.clienteEmpresaService = clienteEmpresaService;
        this.notaFiscalEmpresaService = notaFiscalEmpresaService;
    }

    @GetMapping
    public List<EmpresaResponse> listarEmpresas() {
        return empresaService.listar();
    }

    @GetMapping("/{empresaId}")
    public EmpresaResponse buscarEmpresa(@PathVariable Long empresaId) {
        return empresaService.buscar(empresaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse criarEmpresa(@Valid @RequestBody EmpresaRequest request) {
        return empresaService.criar(request);
    }

    @PutMapping("/{empresaId}")
    public EmpresaResponse atualizarEmpresa(@PathVariable Long empresaId, @Valid @RequestBody EmpresaRequest request) {
        return empresaService.atualizar(empresaId, request);
    }

    @GetMapping("/{empresaId}/produtos")
    public List<ProdutoEmpresaResponse> listarProdutos(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String nome) {
        return produtoEmpresaService.listar(empresaId, nome);
    }

    @GetMapping("/{empresaId}/produtos/paginado")
    public PageResponse<ProdutoEmpresaResponse> listarProdutosPaginado(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String nome) {
        return produtoEmpresaService.listarPaginado(empresaId, page, size, nome);
    }

    @GetMapping("/{empresaId}/produtos/{produtoId}")
    public ProdutoEmpresaResponse buscarProduto(@PathVariable Long empresaId, @PathVariable Long produtoId) {
        return produtoEmpresaService.buscar(empresaId, produtoId);
    }

    @PostMapping("/{empresaId}/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoEmpresaResponse criarProduto(@PathVariable Long empresaId, @Valid @RequestBody ProdutoEmpresaRequest request) {
        return produtoEmpresaService.criar(empresaId, request);
    }

    @PutMapping("/{empresaId}/produtos/{produtoId}")
    public ProdutoEmpresaResponse atualizarProduto(@PathVariable Long empresaId, @PathVariable Long produtoId,
            @Valid @RequestBody ProdutoEmpresaRequest request) {
        return produtoEmpresaService.atualizar(empresaId, produtoId, request);
    }

    @GetMapping("/{empresaId}/clientes")
    public List<ClienteEmpresaResponse> listarClientes(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String nome) {
        return clienteEmpresaService.listar(empresaId, nome);
    }

    @GetMapping("/{empresaId}/clientes/paginado")
    public PageResponse<ClienteEmpresaResponse> listarClientesPaginado(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String nome) {
        return clienteEmpresaService.listarPaginado(empresaId, page, size, nome);
    }

    @GetMapping("/{empresaId}/clientes/{clienteId}")
    public ClienteEmpresaResponse buscarCliente(@PathVariable Long empresaId, @PathVariable Long clienteId) {
        return clienteEmpresaService.buscar(empresaId, clienteId);
    }

    @PostMapping("/{empresaId}/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteEmpresaResponse criarCliente(@PathVariable Long empresaId, @Valid @RequestBody ClienteEmpresaRequest request) {
        return clienteEmpresaService.criar(empresaId, request);
    }

    @PutMapping("/{empresaId}/clientes/{clienteId}")
    public ClienteEmpresaResponse atualizarCliente(@PathVariable Long empresaId, @PathVariable Long clienteId,
            @Valid @RequestBody ClienteEmpresaRequest request) {
        return clienteEmpresaService.atualizar(empresaId, clienteId, request);
    }

    @GetMapping("/{empresaId}/notas")
    public PageResponse<NotaFiscalEmpresaResponse> listarNotas(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return notaFiscalEmpresaService.listar(empresaId, page, size);
    }
}
