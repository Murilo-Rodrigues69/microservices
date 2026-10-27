package isakids.isa.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import isakids.isa.dto.FiscalNfceResponse;
import isakids.isa.dto.ResumoVendaDTO;
import isakids.isa.entities.ItemVenda;
import isakids.isa.entities.Loja;
import isakids.isa.entities.MovimentacaoEstoque;
import isakids.isa.entities.Produto;
import isakids.isa.entities.ProdutoLoja;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;
import isakids.isa.enums.StatusVenda;
import isakids.isa.enums.TipoMovimentoEstoque;
import isakids.isa.repositories.MovimentacaoEstoqueRepository;
import isakids.isa.repositories.ProdutoLojaRepository;
import isakids.isa.repositories.VendaRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class VendaService {

    @Autowired
    VendaRepository vendaRepository;

    @Autowired
    ProdutoLojaRepository produtoLojaRepository;

    @Autowired
    MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Autowired
    AuthenticatedUserService authenticatedUserService;

    @Autowired
    FiscalIntegrationService fiscalIntegrationService;

    @Autowired
    NfceDanfeTermicaService nfceDanfeTermicaService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Transactional
    public Venda abrirVenda(Usuario usuario) {
        if (usuario.getLoja() == null) {
            throw new IllegalStateException("Usuario sem loja vinculada nao pode abrir venda.");
        }

        Venda vendaExistente = buscarVendaAbertaOuPendenteDoUsuario(usuario);
        if (vendaExistente != null) {
            return normalizarVendaAbertaSeNecessario(vendaExistente);
        }

        Venda venda = new Venda();
        venda.setUsuario(usuario);
        venda.setLoja(usuario.getLoja());
        venda.setStatus(StatusVenda.ABERTA);
        venda.setDesconto(BigDecimal.ZERO);
        venda.setTotalBruto(BigDecimal.ZERO);
        venda.setTotalLiquido(BigDecimal.ZERO);
        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda adicionarItem(Long vendaId, String codigoBarras, Integer quantidade) {
        Venda venda = buscarVendaAbertaParaOperacao(vendaId);
        Loja loja = venda.getLoja();
        int quantidadeValidada = validarQuantidade(quantidade);
        String codigoBarrasValidado = validarCodigoBarras(codigoBarras);

        if (venda.getStatus() != StatusVenda.ABERTA) {
            throw new IllegalStateException("Venda nao esta aberta.");
        }

        ProdutoLoja produtoLoja = produtoLojaRepository
                .findByProdutoCodigoBarrasAndLojaIdAndProdutoAtivoTrue(codigoBarrasValidado, loja.getId())
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado na loja"));

        if (produtoLoja.getQuantidadeEstoque() == null
                || produtoLoja.getQuantidadeEstoque() < quantidadeValidada) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente para: " + produtoLoja.getProduto().getDescricao());
        }

        Produto produto = produtoLoja.getProduto();
        ItemVenda item = new ItemVenda(venda, produto, quantidadeValidada, produto.getPrecoVenda());
        venda.adicionarItem(item);

        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda aplicarDesconto(Long vendaId, BigDecimal desconto) {
        Venda venda = buscarVendaAbertaParaOperacao(vendaId);

        if (venda.getStatus() != StatusVenda.ABERTA) {
            throw new IllegalStateException("Nao e possivel aplicar desconto em venda nao aberta.");
        }

        BigDecimal descontoValidado = validarDesconto(venda, desconto);
        venda.setDesconto(descontoValidado);
        venda.recalcularTotais();

        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda finalizarVenda(Long vendaId, FormaPagamento formaPagamento, BigDecimal descontoOpcional) {
        return finalizarVenda(vendaId, formaPagamento, descontoOpcional, null);
    }

    @Transactional
    public Venda finalizarVenda(Long vendaId, FormaPagamento formaPagamento, BigDecimal descontoOpcional,
            String cpfCnpjConsumidor) {
        Venda venda = buscarVendaAbertaParaOperacao(vendaId);

        if (venda.getStatus() != StatusVenda.ABERTA) {
            throw new IllegalStateException("Venda ja foi finalizada ou cancelada.");
        }
        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento e obrigatoria.");
        }

        if (descontoOpcional != null) {
            aplicarDesconto(vendaId, descontoOpcional);
            venda = buscarVendaAbertaParaOperacao(vendaId);
        }

        validarVendaParaFinalizacao(venda);

        for (ItemVenda item : venda.getItens()) {
            Produto produto = item.getProduto();
            ProdutoLoja produtoLoja = buscarEstoqueDaLoja(produto.getId(), venda.getLoja().getId());
            int estoqueAtual = produtoLoja.getQuantidadeEstoque() != null ? produtoLoja.getQuantidadeEstoque() : 0;
            int novoEstoque = estoqueAtual - item.getQuantidade();

            if (novoEstoque < 0) {
                throw new IllegalArgumentException("Estoque insuficiente para: " + produto.getDescricao());
            }

            produtoLoja.setQuantidadeEstoque(novoEstoque);
            produtoLojaRepository.save(produtoLoja);

            MovimentacaoEstoque mov = new MovimentacaoEstoque(
                    produto,
                    venda.getLoja(),
                    TipoMovimentoEstoque.VENDA,
                    item.getQuantidade(),
                    venda.getUsuario(),
                    venda,
                    "VENDA #" + venda.getId());

            movimentacaoEstoqueRepository.save(mov);
        }

        venda.setFormaPagamento(formaPagamento);
        venda.setCpfCnpjConsumidor(normalizeDocumentOrNull(cpfCnpjConsumidor));
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.recalcularTotais();

        return vendaRepository.save(venda);
    }

    public Venda emitirNfce(Long vendaId, String cpfCnpjConsumidor) {
        return emitirNfce(vendaId, cpfCnpjConsumidor, null);
    }

    public Venda emitirNfce(Long vendaId, String cpfCnpjConsumidor, Boolean incluirCpfNaNota) {
        Venda venda = buscarVendaDetalhada(vendaId);
        String consumidor = resolveConsumidorDocumento(venda, cpfCnpjConsumidor, incluirCpfNaNota);

        if (venda.getStatus() != StatusVenda.CONCLUIDA) {
            throw new IllegalStateException("A venda precisa estar concluida antes da emissao da NFC-e.");
        }
        if (possuiRetornoFiscalAtivo(venda)) {
            return venda;
        }

        validarVendaParaEmissao(venda);
        FiscalNfceResponse response = fiscalIntegrationService.emitirNfce(venda, consumidor);

        Venda persisted = transactionTemplate.execute(status -> {
            Venda vendaPersistida = vendaRepository.findById(vendaId)
                    .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada"));
            if (possuiRetornoFiscalAtivo(vendaPersistida)) {
                return vendaPersistida;
            }
            aplicarRetornoFiscal(vendaPersistida, consumidor, response);
            return vendaRepository.save(vendaPersistida);
        });

        if (persisted == null) {
            throw new IllegalStateException("Falha ao persistir o retorno fiscal da venda.");
        }
        return buscarVendaDetalhada(persisted.getId());
    }

    @Transactional
    public Venda removerItem(Long vendaId, Long itemId) {
        Venda venda = buscarVendaAbertaParaOperacao(vendaId);

        if (venda.getStatus() != StatusVenda.ABERTA) {
            throw new IllegalStateException("Venda nao esta aberta.");
        }

        ItemVenda item = venda.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item da venda nao encontrado."));

        venda.getItens().remove(item);
        venda.recalcularTotais();

        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda cancelarVenda(Long vendaId, String motivoOpcional) {
        Venda venda = buscarPorId(vendaId);

        if (venda.getStatus() != StatusVenda.CONCLUIDA) {
            throw new IllegalStateException("So e possivel cancelar vendas concluidas.");
        }

        String motivo = (motivoOpcional == null || motivoOpcional.isBlank()) ? "Sem motivo informado" : motivoOpcional;

        for (ItemVenda item : venda.getItens()) {
            Produto produto = item.getProduto();
            ProdutoLoja produtoLoja = produtoLojaRepository.findByProdutoIdAndLojaId(produto.getId(), venda.getLoja().getId())
                    .orElseGet(() -> new ProdutoLoja(produto, venda.getLoja(), 0));
            int estoqueAtual = produtoLoja.getQuantidadeEstoque() != null ? produtoLoja.getQuantidadeEstoque() : 0;
            produtoLoja.setQuantidadeEstoque(estoqueAtual + item.getQuantidade());
            produtoLojaRepository.save(produtoLoja);

            MovimentacaoEstoque mov = new MovimentacaoEstoque(
                    produto,
                    venda.getLoja(),
                    TipoMovimentoEstoque.AJUSTE_POSITIVO,
                    item.getQuantidade(),
                    venda.getUsuario(),
                    venda,
                    "ESTORNO VENDA #" + vendaId + " - " + motivo);

            movimentacaoEstoqueRepository.save(mov);
        }

        venda.setStatus(StatusVenda.CANCELADA);
        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda buscarVendaPdv(Long id) {
        return normalizarVendaAbertaSeNecessario(buscarVendaDetalhada(id));
    }

    public Venda buscarPorId(Long id) {
        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada"));
        authenticatedUserService.checkCanAccessVenda(venda);
        return venda;
    }

    public byte[] carregarPdfNfce(Long vendaId) {
        Venda venda = requireNfceEmitida(buscarPorId(vendaId));
        return fiscalIntegrationService.baixarPdf(venda);
    }

    public byte[] carregarPdfNfceTermico(Long vendaId) {
        Venda venda = requireNfceEmitida(buscarVendaDetalhada(vendaId));
        byte[] xml = null;
        try {
            xml = fiscalIntegrationService.baixarXml(venda);
        } catch (IllegalStateException ignored) {
        }
        return nfceDanfeTermicaService.gerarDanfe(venda, xml);
    }

    public byte[] carregarXmlNfce(Long vendaId) {
        Venda venda = requireNfceEmitida(buscarPorId(vendaId));
        return fiscalIntegrationService.baixarXml(venda);
    }

    public List<Venda> listarTodas() {
        return listarTodas(null);
    }

    public List<Venda> listarTodas(Long lojaId) {
        if (lojaId == null) {
            return vendaRepository.findAll();
        }
        return vendaRepository.findByLojaId(lojaId);
    }

    public List<Venda> listarPorPeriodo(String inicio, String fim, StatusVenda status) {
        return listarPorPeriodo(inicio, fim, status, null);
    }

    public List<Venda> listarPorPeriodo(String inicio, String fim, StatusVenda status, Long lojaId) {
        LocalDateTime inicioDt = LocalDate.parse(inicio).atStartOfDay();
        LocalDateTime fimDt = LocalDate.parse(fim).atTime(23, 59, 59);

        return vendaRepository.buscarPorPeriodoStatusELoja(inicioDt, fimDt, status, lojaId);
    }

    public List<Venda> listarVendasHoje() {
        return listarVendasHoje(null);
    }

    public List<Venda> listarVendasHoje(Long lojaId) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.atStartOfDay();
        LocalDateTime fim = hoje.atTime(23, 59, 59);

        return vendaRepository.buscarPorPeriodoStatusELoja(inicio, fim, StatusVenda.CONCLUIDA, lojaId);
    }

    public Venda atualizar(Long id, Venda atualizado) {
        Venda existente = vendaRepository.getReferenceById(id);

        if (atualizado.getUsuario() != null) {
            existente.setUsuario(atualizado.getUsuario());
        }
        if (atualizado.getLoja() != null) {
            existente.setLoja(atualizado.getLoja());
        }
        if (atualizado.getStatus() != null) {
            existente.setStatus(atualizado.getStatus());
        }
        if (atualizado.getFormaPagamento() != null) {
            existente.setFormaPagamento(atualizado.getFormaPagamento());
        }
        if (atualizado.getDesconto() != null) {
            existente.setDesconto(validarDesconto(existente, atualizado.getDesconto()));
        }

        return vendaRepository.save(existente);
    }

    public void deletar(Long id) {
        vendaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ResumoVendaDTO resumoPeriodo(LocalDate inicio, LocalDate fim) {
        return resumoPeriodo(inicio, fim, null);
    }

    @Transactional(readOnly = true)
    public ResumoVendaDTO resumoPeriodo(LocalDate inicio, LocalDate fim, Long lojaId) {
        StatusVenda status = StatusVenda.CONCLUIDA;

        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime fimDt = fim.plusDays(1).atStartOfDay().minusNanos(1);

        List<Object[]> lista = vendaRepository.resumoSemItens(inicioDt, fimDt, status, lojaId);
        BigDecimal custoTotal = vendaRepository.custoTotalPeriodo(inicioDt, fimDt, status, lojaId);

        if (lista == null || lista.isEmpty()) {
            ResumoVendaDTO dto = new ResumoVendaDTO();
            dto.setTotalBruto(BigDecimal.ZERO);
            dto.setTotalDesconto(BigDecimal.ZERO);
            dto.setTotalLiquido(BigDecimal.ZERO);
            dto.setQuantidadeVendas(0L);
            return dto;
        }

        Object[] base = lista.get(0);

        BigDecimal totalBruto = toBigDecimal(base[0]);
        BigDecimal totalDesconto = toBigDecimal(base[1]);
        Long quantidadeVendas = toLong(base[2]);

        BigDecimal totalLiquido = totalBruto
                .subtract(totalDesconto)
                .subtract(toBigDecimal(custoTotal));

        ResumoVendaDTO dto = new ResumoVendaDTO();
        dto.setTotalBruto(totalBruto);
        dto.setTotalDesconto(totalDesconto);
        dto.setTotalLiquido(totalLiquido);
        dto.setQuantidadeVendas(quantidadeVendas);

        return dto;
    }

    private Venda buscarVendaDetalhada(Long id) {
        Venda venda = vendaRepository.findDetalhadaById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada"));
        authenticatedUserService.checkCanAccessVenda(venda);
        return venda;
    }

    private Venda buscarVendaAbertaParaOperacao(Long id) {
        return normalizarVendaAbertaSeNecessario(buscarVendaDetalhada(id));
    }

    private Venda buscarVendaAbertaOuPendenteDoUsuario(Usuario usuario) {
        if (usuario.getId() == null || usuario.getLoja() == null || usuario.getLoja().getId() == null) {
            return null;
        }

        return vendaRepository
                .findIdsByUsuarioIdAndLojaIdAndStatusOrStatusNullOrderByDataHoraDesc(
                        usuario.getId(),
                        usuario.getLoja().getId(),
                        StatusVenda.ABERTA)
                .stream()
                .findFirst()
                .flatMap(vendaRepository::findDetalhadaById)
                .orElse(null);
    }

    private Venda normalizarVendaAbertaSeNecessario(Venda venda) {
        if (!isVendaAbertaOuPendente(venda)) {
            return venda;
        }

        if (deveResetarVendaAberta(venda)) {
            resetarVendaAberta(venda);
            return vendaRepository.save(venda);
        }

        boolean changed = false;
        if (venda.getStatus() == null) {
            venda.setStatus(StatusVenda.ABERTA);
            changed = true;
        }
        if (venda.getDesconto() == null) {
            venda.setDesconto(BigDecimal.ZERO);
            changed = true;
        }

        changed |= normalizarItens(venda);
        BigDecimal totalBrutoAnterior = defaultMoney(venda.getTotalBruto());
        BigDecimal totalLiquidoAnterior = defaultMoney(venda.getTotalLiquido());
        venda.recalcularTotais();

        if (!sameMoney(totalBrutoAnterior, venda.getTotalBruto())
                || !sameMoney(totalLiquidoAnterior, venda.getTotalLiquido())) {
            changed = true;
        }

        if (changed) {
            return vendaRepository.save(venda);
        }
        return venda;
    }

    private boolean isVendaAbertaOuPendente(Venda venda) {
        return venda.getStatus() == null || venda.getStatus() == StatusVenda.ABERTA;
    }

    private boolean deveResetarVendaAberta(Venda venda) {
        if (venda.getUsuario() == null || venda.getLoja() == null) {
            return true;
        }
        if (venda.getItens() == null) {
            return true;
        }
        if (venda.getFormaPagamento() != null || StringUtils.hasText(venda.getCpfCnpjConsumidor())) {
            return true;
        }
        if (venda.getNotaFiscalEmpresaId() != null
                || StringUtils.hasText(venda.getNotaFiscalChaveAcesso())
                || venda.getNotaFiscalCStat() != null
                || venda.getNotaFiscalDataEmissao() != null) {
            return true;
        }
        return venda.getItens().stream().anyMatch(this::itemVendaInvalido);
    }

    private boolean normalizarItens(Venda venda) {
        boolean changed = false;
        List<ItemVenda> itens = venda.getItens();
        for (ItemVenda item : itens) {
            BigDecimal subtotalAnterior = item.getSubtotal();
            item.recalcularSubtotal();
            if (!sameMoney(subtotalAnterior, item.getSubtotal())) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean itemVendaInvalido(ItemVenda item) {
        return item == null
                || item.getProduto() == null
                || item.getQuantidade() == null
                || item.getQuantidade() <= 0
                || item.getPrecoUnitario() == null
                || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) < 0;
    }

    private void resetarVendaAberta(Venda venda) {
        venda.setItens(new ArrayList<>());
        venda.setStatus(StatusVenda.ABERTA);
        venda.setFormaPagamento(null);
        venda.setCpfCnpjConsumidor(null);
        venda.setDesconto(BigDecimal.ZERO);
        venda.setTotalBruto(BigDecimal.ZERO);
        venda.setTotalLiquido(BigDecimal.ZERO);
        limparRetornoFiscal(venda);
    }

    private void limparRetornoFiscal(Venda venda) {
        venda.setNotaFiscalEmpresaId(null);
        venda.setNotaFiscalEmpresaNome(null);
        venda.setNotaFiscalModelo(null);
        venda.setNotaFiscalSituacao(null);
        venda.setNotaFiscalCStat(null);
        venda.setNotaFiscalMotivo(null);
        venda.setNotaFiscalChaveAcesso(null);
        venda.setNotaFiscalProtocoloAutorizacao(null);
        venda.setNotaFiscalQrCodeUrl(null);
        venda.setNotaFiscalDataEmissao(null);
    }

    private void validarVendaParaFinalizacao(Venda venda) {
        if (venda.getLoja() == null || venda.getUsuario() == null) {
            throw new IllegalStateException("A venda esta inconsistente e precisa ser aberta novamente.");
        }
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalStateException("A venda precisa ter itens antes da finalizacao.");
        }
        if (venda.getItens().stream().anyMatch(this::itemVendaInvalido)) {
            throw new IllegalStateException("A venda foi resetada por inconsistencias e precisa receber os itens novamente.");
        }
    }

    private void validarVendaParaEmissao(Venda venda) {
        if (venda.getLoja() == null || !StringUtils.hasText(venda.getLoja().getCnpj())) {
            throw new IllegalStateException("A loja da venda precisa ter CNPJ configurado para emitir NFC-e.");
        }
        if (venda.getFormaPagamento() == null) {
            throw new IllegalStateException("A venda precisa ter forma de pagamento antes da emissao.");
        }
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalStateException("A venda precisa ter itens para emitir NFC-e.");
        }

        for (ItemVenda item : venda.getItens()) {
            if (item.getProduto() == null) {
                throw new IllegalStateException("Existe item da venda sem produto vinculado.");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new IllegalStateException("Todos os itens da venda precisam ter quantidade valida.");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Todos os itens da venda precisam ter preco unitario valido.");
            }
            validarProdutoFiscal(item.getProduto());
        }
    }

    private void validarProdutoFiscal(Produto produto) {
        String referencia = StringUtils.hasText(produto.getDescricao())
                ? produto.getDescricao()
                : "produto " + produto.getId();
        List<String> pendencias = new ArrayList<>();

        collectRequiredTextIssue(produto.getDescricao(), "descricao", pendencias);
        collectRequiredDigitsIssue(produto.getNcm(), 8, "ncm", pendencias);
        collectOptionalDigitsIssue(produto.getCest(), 7, "cest", pendencias);
        collectRequiredTextIssue(produto.getOrigem(), "origem", pendencias);
        collectRequiredDigitsIssue(produto.getCfop(), 4, "cfop", pendencias);
        collectRequiredTextIssue(produto.getUnidadeComercial(), "unidadeComercial", pendencias);

        if (!pendencias.isEmpty()) {
            throw new IllegalStateException(
                    "Produto " + referencia + " com configuracao fiscal incompleta para emissao: "
                            + String.join(", ", pendencias) + ".");
        }
    }

    private String resolveConsumidorDocumento(Venda venda, String cpfCnpjConsumidor, Boolean incluirCpfNaNota) {
        if (Boolean.FALSE.equals(incluirCpfNaNota)) {
            return null;
        }

        String consumidorInformado = normalizeDocumentOrNull(cpfCnpjConsumidor);
        if (consumidorInformado != null) {
            return consumidorInformado;
        }

        String consumidorPersistido = normalizeDocumentOrNull(venda.getCpfCnpjConsumidor());
        if (Boolean.TRUE.equals(incluirCpfNaNota) && consumidorPersistido == null) {
            throw new IllegalArgumentException(
                    "cpfCnpjConsumidor deve ser informado para emitir a NFC-e com CPF/CNPJ.");
        }
        return consumidorPersistido;
    }

    private void aplicarRetornoFiscal(Venda venda, String cpfCnpjConsumidor, FiscalNfceResponse response) {
        venda.setCpfCnpjConsumidor(cpfCnpjConsumidor);
        venda.setNotaFiscalEmpresaId(response.getEmpresaId());
        venda.setNotaFiscalEmpresaNome(response.getEmpresaNome());
        venda.setNotaFiscalModelo(response.getModelo());
        venda.setNotaFiscalSituacao(resolveSituacaoFiscal(response.getCStat()));
        venda.setNotaFiscalCStat(response.getCStat());
        venda.setNotaFiscalMotivo(trimToNull(response.getMotivo()));
        venda.setNotaFiscalChaveAcesso(trimToNull(response.getChaveAcesso()));
        venda.setNotaFiscalProtocoloAutorizacao(trimToNull(response.getProtocoloAutorizacao()));
        venda.setNotaFiscalQrCodeUrl(trimToNull(response.getQrCodeUrl()));
        if (StringUtils.hasText(response.getChaveAcesso())) {
            venda.setNotaFiscalDataEmissao(LocalDateTime.now());
        }
    }

    private Venda requireNfceEmitida(Venda venda) {
        if (venda.getNotaFiscalEmpresaId() == null
                && !StringUtils.hasText(venda.getNotaFiscalChaveAcesso())) {
            throw new IllegalStateException("A venda ainda nao possui NFC-e emitida.");
        }
        return venda;
    }

    private String resolveSituacaoFiscal(Integer cStat) {
        if (cStat == null) {
            return "DESCONHECIDA";
        }
        return switch (cStat) {
            case 100, 150 -> "AUTORIZADA";
            case 101, 135, 151, 155 -> "CANCELADA";
            default -> {
                if (cStat >= 500) {
                    yield "ERRO";
                }
                if (cStat >= 200) {
                    yield "NAO_AUTORIZADA";
                }
                yield "PENDENTE";
            }
        };
    }

    private boolean possuiRetornoFiscalAtivo(Venda venda) {
        if (venda.getNotaFiscalEmpresaId() == null
                && !StringUtils.hasText(venda.getNotaFiscalChaveAcesso())) {
            return false;
        }

        String situacao = trimToNull(venda.getNotaFiscalSituacao());
        if (situacao == null) {
            return StringUtils.hasText(venda.getNotaFiscalChaveAcesso());
        }

        return "AUTORIZADA".equalsIgnoreCase(situacao)
                || "PENDENTE".equalsIgnoreCase(situacao);
    }

    private ProdutoLoja buscarEstoqueDaLoja(Long produtoId, Long lojaId) {
        return produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseThrow(() -> new EntityNotFoundException("Produto sem estoque cadastrado para a loja."));
    }

    private int validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        return quantidade;
    }

    private String validarCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            throw new IllegalArgumentException("codigoBarras e obrigatorio.");
        }
        return codigoBarras.trim();
    }

    private BigDecimal validarDesconto(Venda venda, BigDecimal desconto) {
        BigDecimal descontoValidado = desconto != null ? desconto : BigDecimal.ZERO;
        if (descontoValidado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Desconto nao pode ser negativo.");
        }
        if (descontoValidado.compareTo(venda.getTotalBruto()) > 0) {
            throw new IllegalArgumentException("Desconto nao pode ser maior que o total bruto da venda.");
        }
        return descontoValidado;
    }

    private String normalizeDocumentOrNull(String document) {
        if (!StringUtils.hasText(document)) {
            return null;
        }
        String normalized = document.replaceAll("\\D", "");
        if (normalized.length() != 11 && normalized.length() != 14) {
            throw new IllegalArgumentException("cpfCnpjConsumidor deve ter 11 ou 14 digitos.");
        }
        return normalized;
    }

    private void collectRequiredDigitsIssue(String value, int expectedSize, String fieldName, List<String> pendencias) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            pendencias.add(fieldName);
            return;
        }
        String normalized = trimmed.replaceAll("\\D", "");
        if (normalized.length() != expectedSize) {
            pendencias.add(fieldName + " invalido");
        }
    }

    private void collectOptionalDigitsIssue(String value, int expectedSize, String fieldName, List<String> pendencias) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return;
        }
        String normalized = trimmed.replaceAll("\\D", "");
        if (normalized.length() != expectedSize) {
            pendencias.add(fieldName + " invalido");
        }
    }

    private void collectRequiredTextIssue(String value, String fieldName, List<String> pendencias) {
        if (!StringUtils.hasText(value)) {
            pendencias.add(fieldName);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        throw new IllegalArgumentException("Valor nao numerico para BigDecimal: " + value);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalArgumentException("Valor nao numerico para Long: " + value);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean sameMoney(BigDecimal left, BigDecimal right) {
        return defaultMoney(left).compareTo(defaultMoney(right)) == 0;
    }
}
