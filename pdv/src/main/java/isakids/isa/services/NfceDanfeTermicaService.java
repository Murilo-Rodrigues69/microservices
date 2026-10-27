package isakids.isa.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.Barcode128;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import isakids.isa.entities.ItemVenda;
import isakids.isa.entities.Loja;
import isakids.isa.entities.Produto;
import isakids.isa.entities.Usuario;
import isakids.isa.entities.Venda;
import isakids.isa.enums.FormaPagamento;

@Service
public class NfceDanfeTermicaService {

    private static final float MM_TO_POINTS = 72f / 25.4f;
    private static final float PAGE_WIDTH = 80f * MM_TO_POINTS;
    private static final float PAGE_MARGIN = 10f;
    private static final int WRAP_WIDTH = 38;

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.COURIER_BOLD, 9f);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.COURIER_BOLD, 7f);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.COURIER, 7f);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.COURIER, 6f);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.COURIER_BOLD, 8f);

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("R$ #,##0.00",
            DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
    private static final DecimalFormat QUANTITY_FORMAT = new DecimalFormat("#,##0.###",
            DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] gerarDanfe(Venda venda, byte[] xmlBytes) {
        DanfeTermicaData data = buildData(venda, xmlBytes);
        Rectangle pageSize = new Rectangle(PAGE_WIDTH, estimatePageHeight(data));
        Document document = new Document(pageSize, PAGE_MARGIN, PAGE_MARGIN, 8f, 8f);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            document.open();

            addCentered(document, "DANFE NFC-e", FONT_TITLE, 0f, 1f);
            addCentered(document, "Documento Auxiliar da Nota Fiscal", FONT_SUBTITLE, 0f, 0f);
            addCentered(document, "de Consumidor Eletronica", FONT_SUBTITLE, 0f, 0f);
            addCentered(document, "Nao permite aproveitamento de credito de ICMS", FONT_SMALL, 1f, 4f);

            addSeparator(document);
            addCentered(document, defaultText(data.emitenteNome(), "EMITENTE NAO INFORMADO"), FONT_SUBTITLE, 0f, 0f);
            addCentered(document, "CNPJ: " + defaultText(formatCpfCnpj(data.emitenteCnpj()), "NAO INFORMADO"),
                    FONT_BODY, 0f, 0f);
            if (StringUtils.hasText(data.emitenteIe())) {
                addCentered(document, "IE: " + data.emitenteIe(), FONT_BODY, 0f, 0f);
            }
            addWrappedCentered(document, data.emitenteEndereco(), FONT_BODY);

            addSeparator(document);
            addLine(document, "Venda ISA: " + defaultText(toText(venda.getId()), "-"), FONT_BODY);
            if (StringUtils.hasText(data.numero()) || StringUtils.hasText(data.serie())) {
                addLine(document, "NFC-e: " + defaultText(data.numero(), "-")
                        + "   Serie: " + defaultText(data.serie(), "-"), FONT_BODY);
            }
            if (data.dataEmissao() != null) {
                addLine(document, "Emissao: " + DATE_TIME_FORMATTER.format(data.dataEmissao()), FONT_BODY);
            }
            addLine(document, "Situacao: " + defaultText(data.situacao(), "DESCONHECIDA"), FONT_BODY);
            if (StringUtils.hasText(data.protocolo())) {
                addWrappedLabelValue(document, "Protocolo", data.protocolo(), FONT_BODY);
            }
            if (StringUtils.hasText(data.motivo())) {
                addWrappedLabelValue(document, "Motivo", data.motivo(), FONT_BODY);
            }

            addSeparator(document);
            addCentered(document, "CONSUMIDOR", FONT_SUBTITLE, 0f, 1f);
            if (StringUtils.hasText(data.destinatarioNome())) {
                addWrappedLine(document, data.destinatarioNome(), FONT_BODY);
            }
            addLine(document, "Documento: " + defaultText(formatCpfCnpj(data.destinatarioDocumento()),
                    "NAO IDENTIFICADO"), FONT_BODY);
            if (StringUtils.hasText(data.operador())) {
                addWrappedLabelValue(document, "Operador", data.operador(), FONT_BODY);
            }

            addSeparator(document);
            addCentered(document, "ITENS", FONT_SUBTITLE, 0f, 1f);
            for (int i = 0; i < data.itens().size(); i++) {
                DanfeItem item = data.itens().get(i);
                addWrappedLine(document, (i + 1) + ". " + defaultText(item.descricao(), "ITEM SEM DESCRICAO"),
                        FONT_SUBTITLE);
                addLine(document, formatItemValues(item), FONT_BODY);
                if (StringUtils.hasText(item.codigo())) {
                    addWrappedLabelValue(document, "Codigo", item.codigo(), FONT_SMALL);
                }
            }

            addSeparator(document);
            addLine(document, "Total produtos: " + formatMoney(data.totalProdutos()), FONT_BODY);
            if (normalizeMoney(data.desconto()).compareTo(BigDecimal.ZERO) > 0) {
                addLine(document, "Desconto: " + formatMoney(data.desconto()), FONT_BODY);
            }
            addLine(document, "Valor total: " + formatMoney(data.totalNota()), FONT_TOTAL);
            if (normalizeMoney(data.totalTributos()).compareTo(BigDecimal.ZERO) > 0) {
                addLine(document, "Trib aprox: " + formatMoney(data.totalTributos()), FONT_BODY);
            }

            addSeparator(document);
            addCentered(document, "PAGAMENTOS", FONT_SUBTITLE, 0f, 1f);
            for (DanfePagamento pagamento : data.pagamentos()) {
                addWrappedLabelValue(document, pagamento.descricao(), formatMoney(pagamento.valor()), FONT_BODY);
            }
            if (normalizeMoney(data.troco()).compareTo(BigDecimal.ZERO) > 0) {
                addLine(document, "Troco: " + formatMoney(data.troco()), FONT_BODY);
            }

            if (StringUtils.hasText(data.chaveAcesso())) {
                addSeparator(document);
                addCentered(document, "CHAVE DE ACESSO", FONT_SUBTITLE, 0f, 1f);
                addWrappedLine(document, formatAccessKey(data.chaveAcesso()), FONT_BODY);
                addBarcode(document, writer, data.chaveAcesso());
            }

            if (StringUtils.hasText(data.qrCodeUrl())) {
                addSeparator(document);
                addCentered(document, "CONSULTA NFC-e", FONT_SUBTITLE, 0f, 1f);
                addWrappedLine(document, data.qrCodeUrl(), FONT_SMALL);
            }

            if (StringUtils.hasText(data.observacoes())) {
                addSeparator(document);
                addCentered(document, "OBSERVACOES", FONT_SUBTITLE, 0f, 1f);
                addWrappedLine(document, data.observacoes(), FONT_BODY);
            }

            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Erro ao gerar DANFE NFC-e termica.", e);
        }
    }

    private DanfeTermicaData buildData(Venda venda, byte[] xmlBytes) {
        DanfeTermicaData fallback = fallbackData(venda);
        if (xmlBytes == null || xmlBytes.length == 0) {
            return fallback;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document xml = builder.parse(new ByteArrayInputStream(xmlBytes));
            XPath xpath = XPathFactory.newInstance().newXPath();

            String emitenteNome = firstNonBlank(text(xpath, xml, "//*[local-name()='emit']/*[local-name()='xNome']"),
                    fallback.emitenteNome());
            String emitenteCnpj = firstNonBlank(text(xpath, xml, "//*[local-name()='emit']/*[local-name()='CNPJ']"),
                    fallback.emitenteCnpj());
            String emitenteIe = firstNonBlank(text(xpath, xml, "//*[local-name()='emit']/*[local-name()='IE']"),
                    fallback.emitenteIe());
            String emitenteEndereco = firstNonBlank(resolveEnderecoEmitente(xpath, xml), fallback.emitenteEndereco());
            String numero = firstNonBlank(text(xpath, xml, "//*[local-name()='ide']/*[local-name()='nNF']"),
                    fallback.numero());
            String serie = firstNonBlank(text(xpath, xml, "//*[local-name()='ide']/*[local-name()='serie']"),
                    fallback.serie());
            LocalDateTime emissao = firstNonBlankDateTime(
                    text(xpath, xml, "//*[local-name()='ide']/*[local-name()='dhEmi']"),
                    fallback.dataEmissao());
            String destinatarioNome = firstNonBlank(
                    text(xpath, xml, "//*[local-name()='dest']/*[local-name()='xNome']"),
                    fallback.destinatarioNome());
            String destinatarioDocumento = firstNonBlank(
                    text(xpath, xml, "//*[local-name()='dest']/*[local-name()='CPF']"),
                    text(xpath, xml, "//*[local-name()='dest']/*[local-name()='CNPJ']"),
                    fallback.destinatarioDocumento());
            List<DanfeItem> itens = resolveItens(xpath, xml, fallback.itens());
            BigDecimal totalProdutos = firstNonZeroDecimal(
                    decimal(xpath, xml, "//*[local-name()='ICMSTot']/*[local-name()='vProd']"),
                    fallback.totalProdutos());
            BigDecimal desconto = firstNonZeroDecimal(
                    decimal(xpath, xml, "//*[local-name()='ICMSTot']/*[local-name()='vDesc']"),
                    fallback.desconto());
            BigDecimal totalNota = firstNonZeroDecimal(
                    decimal(xpath, xml, "//*[local-name()='ICMSTot']/*[local-name()='vNF']"),
                    fallback.totalNota());
            BigDecimal totalTributos = firstNonZeroDecimal(
                    decimal(xpath, xml, "//*[local-name()='ICMSTot']/*[local-name()='vTotTrib']"),
                    sumTributos(itens, fallback.totalTributos()));
            BigDecimal troco = firstNonZeroDecimal(
                    decimal(xpath, xml, "//*[local-name()='pag']/*[local-name()='vTroco']"),
                    fallback.troco());
            List<DanfePagamento> pagamentos = resolvePagamentos(xpath, xml, fallback.pagamentos());
            String chaveAcesso = firstNonBlank(resolveChaveAcesso(xpath, xml), fallback.chaveAcesso());
            String protocolo = firstNonBlank(
                    text(xpath, xml, "//*[local-name()='protNFe']/*[local-name()='infProt']/*[local-name()='nProt']"),
                    fallback.protocolo());
            String situacao = firstNonBlank(
                    resolveSituacaoFiscal(text(xpath, xml,
                            "//*[local-name()='protNFe']/*[local-name()='infProt']/*[local-name()='cStat']")),
                    fallback.situacao());
            String motivo = firstNonBlank(
                    text(xpath, xml, "//*[local-name()='protNFe']/*[local-name()='infProt']/*[local-name()='xMotivo']"),
                    fallback.motivo());
            String qrCodeUrl = firstNonBlank(
                    text(xpath, xml, "//*[local-name()='infNFeSupl']/*[local-name()='qrCode']"),
                    fallback.qrCodeUrl());
            String observacoes = firstNonBlank(
                    text(xpath, xml, "//*[local-name()='infAdic']/*[local-name()='infCpl']"),
                    fallback.observacoes());

            return new DanfeTermicaData(
                    emitenteNome,
                    emitenteCnpj,
                    emitenteIe,
                    emitenteEndereco,
                    numero,
                    serie,
                    emissao,
                    destinatarioNome,
                    destinatarioDocumento,
                    fallback.operador(),
                    itens,
                    pagamentos,
                    totalProdutos,
                    desconto,
                    totalNota,
                    totalTributos,
                    troco,
                    chaveAcesso,
                    protocolo,
                    situacao,
                    motivo,
                    qrCodeUrl,
                    observacoes);
        } catch (Exception e) {
            return fallback;
        }
    }

    private DanfeTermicaData fallbackData(Venda venda) {
        Loja loja = venda.getLoja();
        Usuario usuario = venda.getUsuario();
        List<DanfeItem> itens = new ArrayList<>();
        for (ItemVenda itemVenda : venda.getItens()) {
            Produto produto = itemVenda.getProduto();
            itens.add(new DanfeItem(
                    produto != null ? produto.getCodigoInterno() : null,
                    produto != null ? produto.getDescricao() : null,
                    produto != null ? produto.getUnidadeComercial() : null,
                    itemVenda.getQuantidade() != null ? BigDecimal.valueOf(itemVenda.getQuantidade()) : BigDecimal.ZERO,
                    itemVenda.getPrecoUnitario(),
                    itemVenda.getSubtotal(),
                    BigDecimal.ZERO));
        }

        List<DanfePagamento> pagamentos = List.of(new DanfePagamento(
                resolveFormaPagamento(venda.getFormaPagamento()),
                normalizeMoney(venda.getTotalLiquido())));

        return new DanfeTermicaData(
                loja != null ? loja.getNome() : null,
                loja != null ? loja.getCnpj() : null,
                loja != null ? loja.getInscricaoEstadual() : null,
                loja != null ? loja.getCodigo() : null,
                null,
                null,
                firstNonBlankDateTime(null, firstNonBlankDateTime(venda.getNotaFiscalDataEmissao() != null
                        ? venda.getNotaFiscalDataEmissao().toString()
                        : null, venda.getDataHora())),
                null,
                venda.getCpfCnpjConsumidor(),
                usuario != null ? usuario.getNome() : null,
                itens,
                pagamentos,
                normalizeMoney(venda.getTotalBruto()),
                normalizeMoney(venda.getDesconto()),
                normalizeMoney(venda.getTotalLiquido()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                venda.getNotaFiscalChaveAcesso(),
                venda.getNotaFiscalProtocoloAutorizacao(),
                venda.getNotaFiscalSituacao(),
                venda.getNotaFiscalMotivo(),
                venda.getNotaFiscalQrCodeUrl(),
                null);
    }

    private List<DanfeItem> resolveItens(XPath xpath, org.w3c.dom.Document xml, List<DanfeItem> fallback)
            throws Exception {
        NodeList nodes = (NodeList) xpath.evaluate("//*[local-name()='det']", xml, XPathConstants.NODESET);
        if (nodes == null || nodes.getLength() == 0) {
            return fallback;
        }

        List<DanfeItem> itens = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            itens.add(new DanfeItem(
                    text(xpath, node, "./*[local-name()='prod']/*[local-name()='cProd']"),
                    text(xpath, node, "./*[local-name()='prod']/*[local-name()='xProd']"),
                    text(xpath, node, "./*[local-name()='prod']/*[local-name()='uCom']"),
                    decimal(xpath, node, "./*[local-name()='prod']/*[local-name()='qCom']"),
                    decimal(xpath, node, "./*[local-name()='prod']/*[local-name()='vUnCom']"),
                    decimal(xpath, node, "./*[local-name()='prod']/*[local-name()='vProd']"),
                    decimal(xpath, node, "./*[local-name()='imposto']/*[local-name()='vTotTrib']")));
        }
        return itens;
    }

    private List<DanfePagamento> resolvePagamentos(XPath xpath, org.w3c.dom.Document xml, List<DanfePagamento> fallback)
            throws Exception {
        NodeList nodes = (NodeList) xpath.evaluate("//*[local-name()='pag']/*[local-name()='detPag']",
                xml, XPathConstants.NODESET);
        if (nodes == null || nodes.getLength() == 0) {
            return fallback;
        }

        List<DanfePagamento> pagamentos = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            pagamentos.add(new DanfePagamento(
                    resolveCodigoPagamento(text(xpath, node, "./*[local-name()='tPag']")),
                    decimal(xpath, node, "./*[local-name()='vPag']")));
        }
        return pagamentos;
    }

    private String resolveEnderecoEmitente(XPath xpath, org.w3c.dom.Document xml) throws Exception {
        String logradouro = text(xpath, xml, "//*[local-name()='emit']/*[local-name()='enderEmit']/*[local-name()='xLgr']");
        String numero = text(xpath, xml, "//*[local-name()='emit']/*[local-name()='enderEmit']/*[local-name()='nro']");
        String bairro = text(xpath, xml, "//*[local-name()='emit']/*[local-name()='enderEmit']/*[local-name()='xBairro']");
        String municipio = text(xpath, xml, "//*[local-name()='emit']/*[local-name()='enderEmit']/*[local-name()='xMun']");
        String uf = text(xpath, xml, "//*[local-name()='emit']/*[local-name()='enderEmit']/*[local-name()='UF']");
        String cep = text(xpath, xml, "//*[local-name()='emit']/*[local-name()='enderEmit']/*[local-name()='CEP']");

        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, logradouro);
        appendAddressPart(builder, numero);
        appendAddressPart(builder, bairro);
        appendAddressPart(builder, municipio);
        appendAddressPart(builder, uf);
        appendAddressPart(builder, formatCep(cep));
        return trimToNull(builder.toString());
    }

    private String resolveChaveAcesso(XPath xpath, org.w3c.dom.Document xml) throws Exception {
        Node node = (Node) xpath.evaluate("//*[local-name()='infNFe']", xml, XPathConstants.NODE);
        if (node == null || node.getAttributes() == null || node.getAttributes().getNamedItem("Id") == null) {
            return null;
        }
        String id = trimToNull(node.getAttributes().getNamedItem("Id").getNodeValue());
        if (!StringUtils.hasText(id)) {
            return null;
        }
        return id.startsWith("NFe") ? id.substring(3) : id;
    }

    private String resolveSituacaoFiscal(String cStat) {
        if (!StringUtils.hasText(cStat)) {
            return null;
        }

        try {
            int codigo = Integer.parseInt(cStat.trim());
            return switch (codigo) {
                case 100, 150 -> "AUTORIZADA";
                case 101, 135, 151, 155 -> "CANCELADA";
                default -> {
                    if (codigo >= 500) {
                        yield "ERRO";
                    }
                    if (codigo >= 200) {
                        yield "NAO_AUTORIZADA";
                    }
                    yield "PENDENTE";
                }
            };
        } catch (NumberFormatException e) {
            return trimToNull(cStat);
        }
    }

    private float estimatePageHeight(DanfeTermicaData data) {
        int lineCount = 38;
        lineCount += wrappedLineCount(data.emitenteEndereco());
        lineCount += wrappedLineCount(data.motivo());
        lineCount += wrappedLineCount(data.destinatarioNome());
        lineCount += wrappedLineCount(data.observacoes());
        lineCount += wrappedLineCount(data.qrCodeUrl());
        lineCount += wrappedLineCount(formatAccessKey(data.chaveAcesso()));

        for (DanfeItem item : data.itens()) {
            lineCount += 2;
            lineCount += wrappedLineCount((item.descricao() != null ? item.descricao().length() : 0) > 0
                    ? item.descricao()
                    : "ITEM SEM DESCRICAO");
        }

        lineCount += data.pagamentos().size();
        return Math.max(420f, 22f + (lineCount * 9f));
    }

    private int wrappedLineCount(String value) {
        return wrap(value, WRAP_WIDTH).size();
    }

    private void addBarcode(Document document, PdfWriter writer, String chaveAcesso) throws Exception {
        Barcode128 barcode = new Barcode128();
        barcode.setCode(chaveAcesso);
        barcode.setBarHeight(22f);
        barcode.setX(0.75f);
        barcode.setFont(null);

        Image image = barcode.createImageWithBarcode(writer.getDirectContent(), null, null);
        image.scaleToFit(PAGE_WIDTH - (PAGE_MARGIN * 2), 34f);
        image.setAlignment(Element.ALIGN_CENTER);
        image.setSpacingBefore(2f);
        image.setSpacingAfter(2f);
        document.add(image);
    }

    private void addSeparator(Document document) throws Exception {
        addLine(document, "--------------------------------------", FONT_BODY);
    }

    private void addCentered(Document document, String text, Font font, float spacingBefore, float spacingAfter)
            throws Exception {
        if (!StringUtils.hasText(text)) {
            return;
        }
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setLeading(font.getSize() + 1f);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        document.add(paragraph);
    }

    private void addLine(Document document, String text, Font font) throws Exception {
        if (!StringUtils.hasText(text)) {
            return;
        }
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        paragraph.setLeading(font.getSize() + 1f);
        paragraph.setSpacingBefore(0f);
        paragraph.setSpacingAfter(0f);
        document.add(paragraph);
    }

    private void addWrappedCentered(Document document, String text, Font font) throws Exception {
        for (String line : wrap(text, WRAP_WIDTH)) {
            addCentered(document, line, font, 0f, 0f);
        }
    }

    private void addWrappedLine(Document document, String text, Font font) throws Exception {
        for (String line : wrap(text, WRAP_WIDTH)) {
            addLine(document, line, font);
        }
    }

    private void addWrappedLabelValue(Document document, String label, String value, Font font) throws Exception {
        String safeLabel = defaultText(label, "");
        List<String> lines = wrap(safeLabel + ": " + defaultText(value, "-"), WRAP_WIDTH);
        for (String line : lines) {
            addLine(document, line, font);
        }
    }

    private String formatItemValues(DanfeItem item) {
        return formatQuantity(item.quantidade())
                + " " + defaultText(item.unidade(), "UN")
                + " x " + formatMoney(item.valorUnitario())
                + " = " + formatMoney(item.total());
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(normalizeMoney(value));
    }

    private String formatQuantity(BigDecimal value) {
        return QUANTITY_FORMAT.format(normalizeMoney(value));
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal sumTributos(List<DanfeItem> itens, BigDecimal fallback) {
        BigDecimal total = BigDecimal.ZERO;
        for (DanfeItem item : itens) {
            total = total.add(normalizeMoney(item.tributos()));
        }
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : normalizeMoney(fallback);
    }

    private String formatAccessKey(String chaveAcesso) {
        if (!StringUtils.hasText(chaveAcesso)) {
            return null;
        }
        String digits = chaveAcesso.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return chaveAcesso;
        }

        List<String> groups = new ArrayList<>();
        for (int i = 0; i < digits.length(); i += 4) {
            groups.add(digits.substring(i, Math.min(i + 4, digits.length())));
        }
        return String.join(" ", groups);
    }

    private String formatCpfCnpj(String value) {
        String digits = value != null ? value.replaceAll("\\D", "") : "";
        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (digits.length() == 14) {
            return digits.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return trimToNull(value);
    }

    private String formatCep(String value) {
        String digits = value != null ? value.replaceAll("\\D", "") : "";
        if (digits.length() == 8) {
            return digits.replaceFirst("(\\d{5})(\\d{3})", "$1-$2");
        }
        return trimToNull(value);
    }

    private String resolveFormaPagamento(FormaPagamento formaPagamento) {
        if (formaPagamento == null) {
            return "FORMA NAO INFORMADA";
        }

        return switch (formaPagamento) {
            case DINHEIRO -> "DINHEIRO";
            case PIX -> "PIX";
            case CARTAO_CREDITO -> "CARTAO CREDITO";
            case CARTAO_DEBITO -> "CARTAO DEBITO";
        };
    }

    private String resolveCodigoPagamento(String codigo) {
        if (!StringUtils.hasText(codigo)) {
            return "FORMA NAO INFORMADA";
        }

        return switch (codigo.trim()) {
            case "01" -> "DINHEIRO";
            case "02" -> "CHEQUE";
            case "03" -> "CARTAO CREDITO";
            case "04" -> "CARTAO DEBITO";
            case "05" -> "CREDITO LOJA";
            case "10" -> "VALE ALIMENTACAO";
            case "11" -> "VALE REFEICAO";
            case "12" -> "VALE PRESENTE";
            case "13" -> "VALE COMBUSTIVEL";
            case "15" -> "BOLETO";
            case "16" -> "DEPOSITO";
            case "17" -> "PIX";
            case "18" -> "TRANSFERENCIA";
            case "19" -> "PROGRAMA FIDELIDADE";
            case "90" -> "SEM PAGAMENTO";
            case "99" -> "OUTROS";
            default -> "PAGAMENTO " + codigo.trim();
        };
    }

    private List<String> wrap(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        int index = 0;
        while (index < normalized.length()) {
            int end = Math.min(index + maxLength, normalized.length());
            if (end < normalized.length() && normalized.charAt(end) != ' ') {
                int lastSpace = normalized.lastIndexOf(' ', end);
                if (lastSpace > index) {
                    end = lastSpace;
                }
            }
            String line = normalized.substring(index, end).trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
            index = end;
            while (index < normalized.length() && normalized.charAt(index) == ' ') {
                index++;
            }
        }
        return lines.isEmpty() ? List.of(normalized) : lines;
    }

    private String text(XPath xpath, Object item, String expression) throws Exception {
        return trimToNull((String) xpath.evaluate(expression, item, XPathConstants.STRING));
    }

    private BigDecimal decimal(XPath xpath, Object item, String expression) throws Exception {
        String value = text(xpath, item, expression);
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    private LocalDateTime firstNonBlankDateTime(String value, LocalDateTime fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }

        try {
            return OffsetDateTime.parse(value.trim()).toLocalDateTime();
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private BigDecimal firstNonZeroDecimal(BigDecimal primary, BigDecimal fallback) {
        if (primary != null && primary.compareTo(BigDecimal.ZERO) != 0) {
            return primary;
        }
        return normalizeMoney(fallback);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String toText(Object value) {
        return value != null ? value.toString() : null;
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    private record DanfeTermicaData(
            String emitenteNome,
            String emitenteCnpj,
            String emitenteIe,
            String emitenteEndereco,
            String numero,
            String serie,
            LocalDateTime dataEmissao,
            String destinatarioNome,
            String destinatarioDocumento,
            String operador,
            List<DanfeItem> itens,
            List<DanfePagamento> pagamentos,
            BigDecimal totalProdutos,
            BigDecimal desconto,
            BigDecimal totalNota,
            BigDecimal totalTributos,
            BigDecimal troco,
            String chaveAcesso,
            String protocolo,
            String situacao,
            String motivo,
            String qrCodeUrl,
            String observacoes) {
    }

    private record DanfeItem(
            String codigo,
            String descricao,
            String unidade,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal total,
            BigDecimal tributos) {
    }

    private record DanfePagamento(String descricao, BigDecimal valor) {
    }
}
