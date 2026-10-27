package isakids.nfcelab.fiscal;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.util.StringUtils;

import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFPais;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFeConfig;
import com.fincatto.documentofiscal.nfe.NFTipoEmissao;
import com.fincatto.documentofiscal.nfe400.classes.NFEndereco;
import com.fincatto.documentofiscal.nfe400.classes.NFFinalidade;
import com.fincatto.documentofiscal.nfe400.classes.NFModalidadeFrete;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoSituacaoTributariaCOFINS;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoSituacaoTributariaPIS;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaSituacaoOperacionalSimplesNacional;
import com.fincatto.documentofiscal.nfe400.classes.NFOrigem;
import com.fincatto.documentofiscal.nfe400.classes.NFProcessoEmissor;
import com.fincatto.documentofiscal.nfe400.classes.NFProdutoCompoeValorNota;
import com.fincatto.documentofiscal.nfe400.classes.NFRegimeTributario;
import com.fincatto.documentofiscal.nfe400.classes.NFTipo;
import com.fincatto.documentofiscal.nfe400.classes.NFTipoImpressao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIdentificadorLocalDestinoOperacao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIndicadorIEDestinatario;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIndicadorIntermediador;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIndicadorPresencaComprador;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFMeioPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNota;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoCartao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfo;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoDestinatario;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoEmitente;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoFormaPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoInformacoesAdicionais;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoICMSTotal;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoIdentificacao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItem;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImposto;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImpostoCOFINS;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImpostoCOFINSOutrasOperacoes;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImpostoICMS;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImpostoICMSSN102;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImpostoPIS;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemImpostoPISOutrasOperacoes;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemProduto;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoTotal;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoTransporte;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFOperacaoConsumidorFinal;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFTipoIntegracaoPagamento;
import com.fincatto.documentofiscal.nfe400.utils.NFGeraChave;

import isakids.nfcelab.config.NfceIssuerProperties;
import isakids.nfcelab.model.FormaPagamento;
import isakids.nfcelab.model.ProductData;
import isakids.nfcelab.model.RecipientData;
import isakids.nfcelab.model.SaleData;
import isakids.nfcelab.model.SaleItemData;

public class NfceXmlFactory {

    private static final String NCM_FALLBACK = "68101100";
    private static final String CFOP_FALLBACK_INTERNO = "5102";
    private static final String CFOP_FALLBACK_INTERESTADUAL = "6102";
    private static final String UN_FALLBACK = "UN";

    private final NFeConfig nfeConfig;
    private final NfceIssuerProperties issuerProperties;

    public NfceXmlFactory(NFeConfig nfeConfig, NfceIssuerProperties issuerProperties) {
        this.nfeConfig = nfeConfig;
        this.issuerProperties = issuerProperties;
    }

    public NFNota gerarNotaFiscal(SaleData venda, RecipientData destinatario) {
        Objects.requireNonNull(venda, "venda nao pode ser null");
        if (venda.itens() == null || venda.itens().isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um item para montar a " + documentLabel() + ".");
        }

        DFModelo modelo = nfeConfig.getModelo();
        final NFNotaInfoIdentificacao ide = new NFNotaInfoIdentificacao();
        ide.setAmbiente(nfeConfig.getAmbiente());
        ide.setUf(nfeConfig.getCUF());
        ide.setModelo(modelo);
        ide.setProgramaEmissor(NFProcessoEmissor.CONTRIBUINTE);
        ide.setVersaoEmissor("1.0.0");
        ide.setSerie(resolveSerie());
        ide.setNumeroNota(resolveNumeroNota(venda));
        ide.setCodigoRandomico(gerarCNF8Digitos());
        ide.setTipoEmissao(NFTipoEmissao.EMISSAO_NORMAL);
        ide.setFinalidade(NFFinalidade.NORMAL);
        ide.setNaturezaOperacao(resolveNaturezaOperacao());
        ide.setTipo(NFTipo.SAIDA);
        ide.setTipoImpressao(resolveTipoImpressao(modelo));
        ide.setOperacaoConsumidorFinal(NFOperacaoConsumidorFinal.SIM);
        ide.setIndicadorPresencaComprador(NFIndicadorPresencaComprador.OPERACAO_PRESENCIAL);
        ide.setIndIntermed(NFIndicadorIntermediador.OPERACAO_SEM_INTERMEDIADOR);

        ZonedDateTime dhEmi = venda.dataHora() != null
                ? venda.dataHora().atZone(ZoneId.of("America/Sao_Paulo"))
                : ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        ide.setDataHoraEmissao(dhEmi);
        ide.setCodigoMunicipio(requireText(issuerProperties.getCodigoMunicipio(), "nfe.emitente.codigo-municipio"));

        final NFNotaInfoEmitente emit = new NFNotaInfoEmitente();
        emit.setCnpj(onlyDigits(requireText(issuerProperties.getCnpj(), "nfe.emitente.cnpj")));
        emit.setRazaoSocial(requireText(issuerProperties.getRazaoSocial(), "nfe.emitente.razao-social"));
        emit.setNomeFantasia(requireText(issuerProperties.getNomeFantasia(), "nfe.emitente.nome-fantasia"));
        emit.setInscricaoEstadual(requireText(issuerProperties.getInscricaoEstadual(), "nfe.emitente.inscricao-estadual"));
        emit.setRegimeTributario(NFRegimeTributario.SIMPLES_NACIONAL);

        final NFEndereco endEmit = new NFEndereco();
        endEmit.setCep(onlyDigits(requireText(issuerProperties.getCep(), "nfe.emitente.cep")));
        endEmit.setLogradouro(requireText(issuerProperties.getLogradouro(), "nfe.emitente.logradouro"));
        endEmit.setNumero(requireText(issuerProperties.getNumero(), "nfe.emitente.numero"));
        endEmit.setBairro(requireText(issuerProperties.getBairro(), "nfe.emitente.bairro"));
        endEmit.setCodigoMunicipio(requireText(issuerProperties.getCodigoMunicipio(), "nfe.emitente.codigo-municipio"));
        endEmit.setDescricaoMunicipio(requireText(issuerProperties.getDescricaoMunicipio(), "nfe.emitente.descricao-municipio"));
        endEmit.setUf(nfeConfig.getCUF());
        endEmit.setCodigoPais(DFPais.BRASIL);
        endEmit.setDescricaoPais("BRASIL");
        emit.setEndereco(endEmit);

        NFNotaInfoDestinatario dest = montarDestinatario(destinatario);
        NFIdentificadorLocalDestinoOperacao localDestinoOperacao = resolveLocalDestinoOperacao(destinatario);
        ide.setIdentificadorLocalDestinoOperacao(localDestinoOperacao);

        final List<NFNotaInfoItem> itens = new ArrayList<>();
        final List<BigDecimal> descontosPorItem = venda.descontosPorItem();
        int nItem = 1;
        BigDecimal totalProdutos = venda.totalProdutos();

        for (int indice = 0; indice < venda.itens().size(); indice++) {
            SaleItemData itemVenda = venda.itens().get(indice);
            ProductData produto = itemVenda.produto();
            if (produto == null) {
                throw new IllegalArgumentException("Todos os itens precisam informar o produto.");
            }

            final NFNotaInfoItem item = new NFNotaInfoItem();
            int numeroItemAtual = nItem;
            item.setNumeroItem(nItem++);

            final NFNotaInfoItemProduto prod = new NFNotaInfoItemProduto();
            prod.setCodigo(String.valueOf(produto.id() != null ? produto.id() : (itemVenda.id() != null ? itemVenda.id() : numeroItemAtual)));

            String descricaoProduto = produto.descricao();
            if (!StringUtils.hasText(descricaoProduto)) {
                descricaoProduto = "PRODUTO";
            }
            if (DFAmbiente.HOMOLOGACAO.equals(nfeConfig.getAmbiente()) && numeroItemAtual == 1) {
                descricaoProduto = "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
            }
            prod.setDescricao(descricaoProduto);
            prod.setNcm(resolveNcm(produto));
            String cest = resolveCest(produto);
            if (StringUtils.hasText(cest)) {
                prod.setCodigoEspecificadorSituacaoTributaria(cest);
            }
            prod.setCfop(resolveCfop(produto, localDestinoOperacao));
            prod.setUnidadeComercial(resolveUnidade(produto));

            BigDecimal qtd = BigDecimal.valueOf(itemVenda.quantidade() != null ? itemVenda.quantidade() : 1);
            BigDecimal valorUnitario = itemVenda.precoUnitario() != null ? itemVenda.precoUnitario() : BigDecimal.ZERO;
            BigDecimal valorTotal = itemVenda.subtotalOrCalculated();
            BigDecimal descontoItem = descontosPorItem.get(indice);

            prod.setQuantidadeComercial(qtd);
            prod.setValorUnitario(valorUnitario);
            prod.setValorTotalBruto(valorTotal);
            if (descontoItem.signum() > 0) {
                prod.setValorDesconto(descontoItem);
            }

            String gtin = normalizarGtinOuNull(produto.codigoBarras());
            prod.setCodigoDeBarras(gtin != null ? gtin : "SEM GTIN");
            prod.setCodigoDeBarrasTributavel(gtin != null ? gtin : "SEM GTIN");
            prod.setUnidadeTributavel(resolveUnidade(produto));
            prod.setQuantidadeTributavel(qtd);
            prod.setValorUnitarioTributavel(valorUnitario);
            prod.setCompoeValorNota(NFProdutoCompoeValorNota.SIM);

            item.setProduto(prod);
            item.setImposto(montarImpostos(produto));

            itens.add(item);
        }

        final NFNotaInfoTotal total = new NFNotaInfoTotal();
        final NFNotaInfoICMSTotal icmsTot = new NFNotaInfoICMSTotal();
        BigDecimal desconto = venda.descontoTotalAplicado();
        BigDecimal totalNfe = venda.valorTotalNota();

        icmsTot.setValorICMSDesonerado(BigDecimal.ZERO);
        icmsTot.setBaseCalculoICMS(BigDecimal.ZERO);
        icmsTot.setValorTotalICMS(BigDecimal.ZERO);
        icmsTot.setBaseCalculoICMSST(BigDecimal.ZERO);
        icmsTot.setValorTotalICMSST(BigDecimal.ZERO);
        icmsTot.setValorTotalDosProdutosServicos(totalProdutos);
        icmsTot.setValorTotalDesconto(desconto);
        icmsTot.setValorTotalFrete(BigDecimal.ZERO);
        icmsTot.setValorTotalSeguro(BigDecimal.ZERO);
        icmsTot.setOutrasDespesasAcessorias(BigDecimal.ZERO);
        icmsTot.setValorICMSPartilhaDestinatario(BigDecimal.ZERO);
        icmsTot.setValorTotalFundoCombatePobreza(BigDecimal.ZERO);
        icmsTot.setValorTotalFundoCombatePobrezaST(BigDecimal.ZERO);
        icmsTot.setValorTotalFundoCombatePobrezaSTRetido(BigDecimal.ZERO);
        icmsTot.setValorTotalIPIDevolvido(BigDecimal.ZERO);
        icmsTot.setValorTotalII(BigDecimal.ZERO);
        icmsTot.setValorTotalIPI(BigDecimal.ZERO);
        icmsTot.setValorPIS(BigDecimal.ZERO);
        icmsTot.setValorCOFINS(BigDecimal.ZERO);
        icmsTot.setValorTotalTributos(BigDecimal.ZERO);
        icmsTot.setValorTotalNFe(totalNfe);
        total.setIcmsTotal(icmsTot);

        final NFNotaInfoPagamento pagamento = new NFNotaInfoPagamento();
        final NFNotaInfoFormaPagamento detalhePagamento = new NFNotaInfoFormaPagamento();
        detalhePagamento.setMeioPagamento(mapMeioPagamento(venda.formaPagamento()));
        detalhePagamento.setValorPagamento(totalNfe);
        if (isPagamentoCartao(venda.formaPagamento())) {
            detalhePagamento.setCartao(montarPagamentoCartaoNaoIntegrado());
        }
        pagamento.setDetalhamentoFormasPagamento(List.of(detalhePagamento));

        final NFNotaInfo info = new NFNotaInfo();
        final NFNotaInfoTransporte transporte = new NFNotaInfoTransporte();
        transporte.setModalidadeFrete(NFModalidadeFrete.SEM_OCORRENCIA_TRANSPORTE);

        info.setTransporte(transporte);
        info.setVersao(BigDecimal.valueOf(4.00));
        info.setIdentificacao(ide);
        info.setEmitente(emit);
        if (dest != null) {
            info.setDestinatario(dest);
        }
        info.setItens(itens);
        info.setTotal(total);
        info.setPagamento(pagamento);
        if (StringUtils.hasText(venda.observacao())) {
            info.setInformacoesAdicionais(montarInformacoesAdicionais(venda.observacao()));
        }

        final NFNota nota = new NFNota();
        nota.setInfo(info);
        NFGeraChave geradorChave = new NFGeraChave(nota);
        info.setIdentificador(geradorChave.getChaveAcesso());
        ide.setDigitoVerificador(geradorChave.getDV());
        return nota;
    }

    private NFNotaInfoDestinatario montarDestinatario(RecipientData destinatario) {
        if (destinatario == null) {
            if (DFModelo.NFE.equals(nfeConfig.getModelo())) {
                throw new IllegalStateException("Configure o destinatario antes de emitir NFe.");
            }
            return null;
        }

        String doc = onlyDigits(destinatario.documento());
        NFNotaInfoDestinatario dest = new NFNotaInfoDestinatario();
        String nomeDestinatario = DFAmbiente.HOMOLOGACAO.equals(nfeConfig.getAmbiente())
                ? "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL"
                : firstNonBlank(destinatario.nome(), "CONSUMIDOR FINAL");

        if (doc.length() == 11) {
            dest.setCpf(doc);
            dest.setRazaoSocial(nomeDestinatario);
        } else if (doc.length() == 14) {
            dest.setCnpj(doc);
            dest.setRazaoSocial(nomeDestinatario);
        } else {
            throw new IllegalArgumentException("CPF/CNPJ do destinatario invalido: " + destinatario.documento());
        }

        aplicarInscricaoEstadual(dest, destinatario.inscricaoEstadual());
        if (StringUtils.hasText(destinatario.email())) {
            dest.setEmail(destinatario.email().trim());
        }
        NFEndereco endDest = new NFEndereco();
        endDest.setLogradouro(firstNonBlank(destinatario.logradouro(), "RUA TESTE"));
        endDest.setNumero(firstNonBlank(destinatario.numero(), "123"));
        endDest.setBairro(firstNonBlank(destinatario.bairro(), "CENTRO"));
        endDest.setCodigoMunicipio(firstNonBlank(destinatario.codigoMunicipio(),
                requireText(issuerProperties.getCodigoMunicipio(), "nfe.emitente.codigo-municipio")));
        endDest.setDescricaoMunicipio(firstNonBlank(destinatario.descricaoMunicipio(),
                requireText(issuerProperties.getDescricaoMunicipio(), "nfe.emitente.descricao-municipio")));
        endDest.setUf(resolveUfDestinatario(destinatario.uf()));
        endDest.setCep(firstNonBlank(onlyDigitsOrNull(destinatario.cep()),
                onlyDigits(requireText(issuerProperties.getCep(), "nfe.emitente.cep"))));
        endDest.setCodigoPais(DFPais.BRASIL);
        endDest.setDescricaoPais("BRASIL");
        dest.setEndereco(endDest);
        return dest;
    }

    private void aplicarInscricaoEstadual(NFNotaInfoDestinatario dest, String inscricaoEstadual) {
        if (!StringUtils.hasText(inscricaoEstadual)) {
            dest.setIndicadorIEDestinatario(NFIndicadorIEDestinatario.NAO_CONTRIBUINTE);
            return;
        }

        if ("ISENTO".equalsIgnoreCase(inscricaoEstadual.trim())) {
            dest.setIndicadorIEDestinatario(NFIndicadorIEDestinatario.CONTRIBUINTE_ISENTO_INSCRICAO_CONTRIBUINTES_ICMS);
            return;
        }

        dest.setIndicadorIEDestinatario(NFIndicadorIEDestinatario.CONTRIBUINTE_ICMS);
        dest.setInscricaoEstadual(onlyDigits(inscricaoEstadual));
    }

    private NFNotaInfoItemImposto montarImpostos(ProductData produto) {
        final NFNotaInfoItemImposto imposto = new NFNotaInfoItemImposto();

        final NFNotaInfoItemImpostoICMS icms = new NFNotaInfoItemImpostoICMS();
        final NFNotaInfoItemImpostoICMSSN102 sn102 = new NFNotaInfoItemImpostoICMSSN102();
        sn102.setOrigem(resolveOrigem(produto));
        sn102.setSituacaoOperacaoSN(NFNotaSituacaoOperacionalSimplesNacional.CSOSN_102);
        icms.setIcmssn102(sn102);
        imposto.setIcms(icms);

        final NFNotaInfoItemImpostoPIS pis = new NFNotaInfoItemImpostoPIS();
        final NFNotaInfoItemImpostoPISOutrasOperacoes pisOutras = new NFNotaInfoItemImpostoPISOutrasOperacoes();
        pisOutras.setSituacaoTributaria(NFNotaInfoSituacaoTributariaPIS.CST_49);
        pisOutras.setValorBaseCalculo(BigDecimal.ZERO);
        pisOutras.setPercentualAliquota(BigDecimal.ZERO);
        pisOutras.setValorTributo(BigDecimal.ZERO);
        pis.setOutrasOperacoes(pisOutras);
        imposto.setPis(pis);

        final NFNotaInfoItemImpostoCOFINS cofins = new NFNotaInfoItemImpostoCOFINS();
        final NFNotaInfoItemImpostoCOFINSOutrasOperacoes cofinsOutras = new NFNotaInfoItemImpostoCOFINSOutrasOperacoes();
        cofinsOutras.setSituacaoTributaria(NFNotaInfoSituacaoTributariaCOFINS.CST_49);
        cofinsOutras.setValorBaseCalculo(BigDecimal.ZERO);
        cofinsOutras.setPercentualCOFINS(BigDecimal.ZERO);
        cofinsOutras.setValorCOFINS(BigDecimal.ZERO);
        cofins.setOutrasOperacoes(cofinsOutras);
        imposto.setCofins(cofins);

        return imposto;
    }

    private String resolveNumeroNota(SaleData venda) {
        if (StringUtils.hasText(venda.numeroNota())) {
            return venda.numeroNota().trim();
        }
        if (venda.vendaId() != null) {
            return String.valueOf(venda.vendaId());
        }
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L);
    }

    private String resolveSerie() {
        return StringUtils.hasText(issuerProperties.getSerie()) ? issuerProperties.getSerie().trim() : "1";
    }

    private String resolveNaturezaOperacao() {
        return StringUtils.hasText(issuerProperties.getNaturezaOperacao())
                ? issuerProperties.getNaturezaOperacao().trim()
                : "VENDA DE MERCADORIA";
    }

    private String resolveNcm(ProductData produto) {
        return StringUtils.hasText(produto.ncm()) ? onlyDigits(produto.ncm()) : NCM_FALLBACK;
    }

    private String resolveCest(ProductData produto) {
        return StringUtils.hasText(produto.cest()) ? onlyDigits(produto.cest()) : null;
    }

    private String resolveCfop(ProductData produto, NFIdentificadorLocalDestinoOperacao localDestinoOperacao) {
        String cfop = StringUtils.hasText(produto.cfop())
                ? onlyDigits(produto.cfop())
                : defaultCfop(localDestinoOperacao);
        validateCfopForDestination(cfop, localDestinoOperacao);
        return cfop;
    }

    private String defaultCfop(NFIdentificadorLocalDestinoOperacao localDestinoOperacao) {
        return switch (localDestinoOperacao) {
            case OPERACAO_INTERESTADUAL -> CFOP_FALLBACK_INTERESTADUAL;
            case OPERACAO_COM_EXTERIOR -> "7102";
            default -> CFOP_FALLBACK_INTERNO;
        };
    }

    private void validateCfopForDestination(String cfop, NFIdentificadorLocalDestinoOperacao localDestinoOperacao) {
        if (!StringUtils.hasText(cfop)) {
            return;
        }
        char primeiroDigito = cfop.charAt(0);
        boolean coerente = switch (localDestinoOperacao) {
            case OPERACAO_INTERNA -> primeiroDigito == '5';
            case OPERACAO_INTERESTADUAL -> primeiroDigito == '6';
            case OPERACAO_COM_EXTERIOR -> primeiroDigito == '7';
        };
        if (!coerente) {
            throw new IllegalArgumentException("CFOP " + cfop + " nao combina com o destino da operacao (" + destinationLabel(localDestinoOperacao)
                    + "). Ajuste a UF do cliente ou o CFOP do produto.");
        }
    }

    private String resolveUnidade(ProductData produto) {
        return StringUtils.hasText(produto.unidadeComercial()) ? produto.unidadeComercial().trim() : UN_FALLBACK;
    }

    private NFOrigem resolveOrigem(ProductData produto) {
        if (!StringUtils.hasText(produto.origem())) {
            return NFOrigem.NACIONAL;
        }
        return NFOrigem.valueOf(produto.origem().trim().toUpperCase(Locale.ROOT));
    }

    private NFTipoImpressao resolveTipoImpressao(DFModelo modelo) {
        return DFModelo.NFCE.equals(modelo) ? NFTipoImpressao.DANFE_NFCE : NFTipoImpressao.DANFE_NORMAL_RETRATO;
    }

    private static String gerarCNF8Digitos() {
        int value = ThreadLocalRandom.current().nextInt(0, 100_000_000);
        return String.format("%08d", value);
    }

    private static NFMeioPagamento mapMeioPagamento(FormaPagamento formaPagamento) {
        if (formaPagamento == null) {
            return NFMeioPagamento.DINHEIRO;
        }
        return switch (formaPagamento) {
            case DINHEIRO -> NFMeioPagamento.DINHEIRO;
            case PIX -> NFMeioPagamento.PIX_ESTATICO;
            case CARTAO_CREDITO -> NFMeioPagamento.CARTAO_CREDITO;
            case CARTAO_DEBITO -> NFMeioPagamento.CARTAO_DEBITO;
        };
    }

    private static boolean isPagamentoCartao(FormaPagamento formaPagamento) {
        return FormaPagamento.CARTAO_CREDITO.equals(formaPagamento)
                || FormaPagamento.CARTAO_DEBITO.equals(formaPagamento);
    }

    private static NFNotaInfoCartao montarPagamentoCartaoNaoIntegrado() {
        NFNotaInfoCartao cartao = new NFNotaInfoCartao();
        cartao.setTipoIntegracao(NFTipoIntegracaoPagamento.SEPARADO);
        return cartao;
    }

    private NFNotaInfoInformacoesAdicionais montarInformacoesAdicionais(String observacao) {
        NFNotaInfoInformacoesAdicionais informacoesAdicionais = new NFNotaInfoInformacoesAdicionais();
        informacoesAdicionais.setInformacoesComplementaresInteresseContribuinte(observacao.trim());
        return informacoesAdicionais;
    }

    private static String normalizarGtinOuNull(String ean) {
        if (!StringUtils.hasText(ean)) {
            return null;
        }
        String value = onlyDigits(ean);
        if (value.length() == 8 || value.length() == 12 || value.length() == 13 || value.length() == 14) {
            if (value.chars().distinct().count() == 1) {
                return null;
            }
            return isGtinValido(value) ? value : null;
        }
        return null;
    }

    private static boolean isGtinValido(String value) {
        int soma = 0;
        boolean pesoTres = true;

        for (int indice = value.length() - 2; indice >= 0; indice--) {
            int digito = Character.digit(value.charAt(indice), 10);
            soma += digito * (pesoTres ? 3 : 1);
            pesoTres = !pesoTres;
        }

        int digitoVerificadorCalculado = (10 - (soma % 10)) % 10;
        int digitoVerificadorInformado = Character.digit(value.charAt(value.length() - 1), 10);
        return digitoVerificadorCalculado == digitoVerificadorInformado;
    }

    private static String onlyDigits(String value) {
        return value.replaceAll("\\D", "");
    }

    private static String onlyDigitsOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    private DFUnidadeFederativa resolveUfDestinatario(String uf) {
        if (!StringUtils.hasText(uf)) {
            return nfeConfig.getCUF();
        }
        return DFUnidadeFederativa.valueOf(uf.trim().toUpperCase(Locale.ROOT));
    }

    private NFIdentificadorLocalDestinoOperacao resolveLocalDestinoOperacao(RecipientData destinatario) {
        if (destinatario == null || !StringUtils.hasText(destinatario.uf())) {
            return NFIdentificadorLocalDestinoOperacao.OPERACAO_INTERNA;
        }
        DFUnidadeFederativa ufDestinatario = resolveUfDestinatario(destinatario.uf());
        return nfeConfig.getCUF().equals(ufDestinatario)
                ? NFIdentificadorLocalDestinoOperacao.OPERACAO_INTERNA
                : NFIdentificadorLocalDestinoOperacao.OPERACAO_INTERESTADUAL;
    }

    private String destinationLabel(NFIdentificadorLocalDestinoOperacao localDestinoOperacao) {
        return switch (localDestinoOperacao) {
            case OPERACAO_INTERNA -> "interna";
            case OPERACAO_INTERESTADUAL -> "interestadual";
            case OPERACAO_COM_EXTERIOR -> "exterior";
        };
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Configure a propriedade " + propertyName + " antes de emitir " + documentLabel() + ".");
        }
        return value.trim();
    }

    private String documentLabel() {
        return DFModelo.NFCE.equals(nfeConfig.getModelo()) ? "NFC-e" : "NF-e";
    }
}
