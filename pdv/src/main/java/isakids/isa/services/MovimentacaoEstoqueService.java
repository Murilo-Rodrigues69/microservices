package isakids.isa.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import isakids.isa.entities.Loja;
import isakids.isa.entities.MovimentacaoEstoque;
import isakids.isa.entities.Produto;
import isakids.isa.entities.ProdutoLoja;
import isakids.isa.entities.Usuario;
import isakids.isa.enums.TipoMovimentoEstoque;
import isakids.isa.repositories.LojaRepository;
import isakids.isa.repositories.MovimentacaoEstoqueRepository;
import isakids.isa.repositories.ProdutoLojaRepository;
import isakids.isa.repositories.ProdutoRepository;
import isakids.isa.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class MovimentacaoEstoqueService {

    @Autowired
    MovimentacaoEstoqueRepository repository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ProdutoLojaRepository produtoLojaRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<MovimentacaoEstoque> listarTodas() {
        return listarTodas(null);
    }

    public List<MovimentacaoEstoque> listarTodas(Long lojaId) {
        if (lojaId != null) {
            return repository.findByLojaId(lojaId);
        }
        return repository.findAll();
    }

    public MovimentacaoEstoque buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimentacao nao encontrada."));
    }

    public MovimentacaoEstoque salvar(MovimentacaoEstoque mov) {
        return repository.save(mov);
    }

    public void deletar(Long id) {
        MovimentacaoEstoque mov = buscarPorId(id);
        repository.delete(mov);
    }

    @Transactional
    public MovimentacaoEstoque registrarEntradaMercadoria(
            Long produtoId,
            Long lojaId,
            Integer quantidade,
            Usuario usuario,
            String observacao
    ) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        Produto produto = buscarProduto(produtoId);
        Loja loja = buscarLoja(lojaId);
        Usuario usuarioOperador = buscarUsuario(usuario);

        ProdutoLoja produtoLoja = produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseGet(() -> new ProdutoLoja(produto, loja, 0));

        int estoqueAtual = produtoLoja.getQuantidadeEstoque() != null ? produtoLoja.getQuantidadeEstoque() : 0;
        produtoLoja.setQuantidadeEstoque(estoqueAtual + quantidade);
        produtoLojaRepository.save(produtoLoja);

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto);
        mov.setLoja(loja);
        mov.setTipo(TipoMovimentoEstoque.ENTRADA);
        mov.setQuantidade(quantidade);
        mov.setDataHora(LocalDateTime.now());
        mov.setUsuario(usuarioOperador);
        mov.setObservacao(
                observacao != null && !observacao.isBlank()
                        ? observacao
                        : "Entrada manual de mercadoria"
        );

        return repository.save(mov);
    }

    @Transactional
    public MovimentacaoEstoque registrarSaidaMercadoria(
            Long produtoId,
            Long lojaId,
            Integer quantidade,
            Usuario usuario,
            String observacao
    ) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        Produto produto = buscarProduto(produtoId);
        Loja loja = buscarLoja(lojaId);
        Usuario usuarioOperador = buscarUsuario(usuario);

        ProdutoLoja produtoLoja = produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseThrow(() -> new EntityNotFoundException("Produto sem estoque cadastrado para a loja: " + lojaId));

        int estoqueAtual = produtoLoja.getQuantidadeEstoque() != null ? produtoLoja.getQuantidadeEstoque() : 0;
        int novoEstoque = estoqueAtual - quantidade;

        if (novoEstoque < 0) {
            throw new IllegalArgumentException("Estoque insuficiente para saida. Estoque atual: " + estoqueAtual);
        }

        produtoLoja.setQuantidadeEstoque(novoEstoque);
        produtoLojaRepository.save(produtoLoja);

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto);
        mov.setLoja(loja);
        mov.setTipo(TipoMovimentoEstoque.AJUSTE_NEGATIVO);
        mov.setQuantidade(quantidade);
        mov.setDataHora(LocalDateTime.now());
        mov.setUsuario(usuarioOperador);
        mov.setObservacao(
                observacao != null && !observacao.isBlank()
                        ? observacao
                        : "Saida manual de mercadoria"
        );

        return repository.save(mov);
    }

    public List<MovimentacaoEstoque> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return listarPorPeriodo(inicio, fim, null);
    }

    public List<MovimentacaoEstoque> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim, Long lojaId) {
        if (inicio == null && fim == null) {
            return listarTodas(lojaId);
        }

        if (inicio != null && fim != null) {
            if (lojaId != null) {
                return repository.findByLojaIdAndDataHoraBetween(lojaId, inicio, fim);
            }
            return repository.findByDataHoraBetween(inicio, fim);
        }

        if (inicio != null) {
            if (lojaId != null) {
                return repository.findByLojaIdAndDataHoraGreaterThanEqual(lojaId, inicio);
            }
            return repository.findByDataHoraGreaterThanEqual(inicio);
        }

        if (lojaId != null) {
            return repository.findByLojaIdAndDataHoraLessThanEqual(lojaId, fim);
        }
        return repository.findByDataHoraLessThanEqual(fim);
    }

    private Produto buscarProduto(Long produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado: " + produtoId));
    }

    private Loja buscarLoja(Long lojaId) {
        if (lojaId == null) {
            throw new IllegalArgumentException("lojaId e obrigatorio");
        }
        return lojaRepository.findById(lojaId)
                .orElseThrow(() -> new EntityNotFoundException("Loja nao encontrada: " + lojaId));
    }

    private Usuario buscarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException("Usuario autenticado e obrigatorio");
        }
        return usuarioRepository.findByIdAndAtivoTrue(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado ou inativo: " + usuario.getId()));
    }
}
