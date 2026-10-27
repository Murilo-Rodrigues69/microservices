package isakids.nfcelab.document;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

@Component
public class NfceDanfeRenderer {

    private static final String NFE_NAMESPACE = "http://www.portalfiscal.inf.br/nfe";

    public byte[] render(String xmlNfce) {
        try {
            return doRender(xmlNfce);
        } catch (Exception e) {
            throw new IllegalStateException("Falha gerando DANFE NFC-e em PDF.", e);
        }
    }

    private byte[] doRender(String xmlNfce) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        org.w3c.dom.Document xmlDoc = parseXmlString(xmlNfce);
        Element infNFe = requireElement(xmlDoc, "infNFe");
        Element ide = requireChild(infNFe, "ide");
        Element emit = requireChild(infNFe, "emit");
        Element enderEmit = requireChild(emit, "enderEmit");
        Element dest = getFirstElement(infNFe, "dest");
        Element icmsTot = requireElement(xmlDoc, "ICMSTot");

        Table mainTable = new Table(UnitValue.createPointArray(new float[] { 320f }))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        Cell container = new Cell()
                .setPadding(10)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f));

        Table headerTab = new Table(UnitValue.createPercentArray(new float[] { 30, 70 })).useAllAvailableWidth();
        headerTab.addCell(new Cell()
                .add(new Paragraph(firstNonBlank(getElementValue(emit, "xFant"), getElementValue(emit, "xNome")))
                        .setBold()
                        .setFontSize(9))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER));

        String cabecalhoTexto = String.format(Locale.ROOT,
                "%s%nCNPJ: %s%n%s, %s, %s%n%s - %s%n%nDANFE NFC-e - Documento Auxiliar%nda Nota Fiscal de Consumidor Eletronica",
                getElementValue(emit, "xNome"),
                formatarCnpjCpf(getElementValue(emit, "CNPJ")),
                getElementValue(enderEmit, "xLgr"),
                firstNonBlank(getElementValue(enderEmit, "nro"), "S/N"),
                getElementValue(enderEmit, "xBairro"),
                getElementValue(enderEmit, "xMun"),
                getElementValue(enderEmit, "UF"));

        headerTab.addCell(new Cell()
                .add(new Paragraph(cabecalhoTexto).setFontSize(7))
                .setBorder(Border.NO_BORDER));
        container.add(headerTab);

        container.add(separator());

        Table prodTab = new Table(UnitValue.createPercentArray(new float[] { 15, 40, 15, 15, 15 }))
                .useAllAvailableWidth();
        for (String header : new String[] { "Cod", "Descricao", "Qtd UN", "Vl Un", "Vl Tot" }) {
            prodTab.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(7))
                    .setBorder(Border.NO_BORDER));
        }

        NodeList dets = xmlDoc.getElementsByTagNameNS(NFE_NAMESPACE, "det");
        for (int i = 0; i < dets.getLength(); i++) {
            Element det = (Element) dets.item(i);
            Element prod = requireChild(det, "prod");
            prodTab.addCell(bodyCell(getElementValue(prod, "cProd"), TextAlignment.LEFT));
            prodTab.addCell(bodyCell(getElementValue(prod, "xProd"), TextAlignment.LEFT));
            prodTab.addCell(bodyCell(
                    String.format(Locale.ROOT, "%s %s", getElementValue(prod, "qCom"), getElementValue(prod, "uCom")),
                    TextAlignment.LEFT));
            prodTab.addCell(bodyCell(getElementValue(prod, "vUnCom"), TextAlignment.RIGHT));
            prodTab.addCell(bodyCell(getElementValue(prod, "vProd"), TextAlignment.RIGHT));
        }
        container.add(prodTab);

        container.add(separator());
        container.add(criarLinhaTotal("Qtd. Total de Itens", String.valueOf(dets.getLength())));
        container.add(criarLinhaTotal("Valor Total R$", getElementValue(icmsTot, "vProd")));

        String desconto = getElementValue(icmsTot, "vDesc");
        if (isPositiveNumber(desconto)) {
            container.add(criarLinhaTotal("Desconto R$", desconto));
        }

        container.add(criarLinhaTotal("VALOR A PAGAR R$", getElementValue(icmsTot, "vNF"), true));

        String vTotTrib = getElementValue(icmsTot, "vTotTrib");
        if (isPositiveNumber(vTotTrib)) {
            container.add(new Paragraph("Informacao dos Tributos Totais Incidentes (Lei Federal 12.741/2012):")
                    .setFontSize(6)
                    .setItalic()
                    .setMarginTop(5));
            container.add(criarLinhaTotal("Total aproximado de tributos R$", vTotTrib));
        }

        container.add(new Paragraph("\nFORMA DE PAGAMENTO").setBold().setFontSize(7));
        NodeList pagamentos = xmlDoc.getElementsByTagNameNS(NFE_NAMESPACE, "detPag");
        for (int i = 0; i < pagamentos.getLength(); i++) {
            Element detPag = (Element) pagamentos.item(i);
            container.add(criarLinhaTotal(
                    getFormaPagamento(getElementValue(detPag, "tPag")),
                    getElementValue(detPag, "vPag")));
        }

        String chave = infNFe.getAttribute("Id");
        if (StringUtils.hasText(chave) && chave.startsWith("NFe")) {
            chave = chave.substring(3);
        }
        String urlConsulta = getExtraValue(xmlDoc, "urlChave");
        String qrCodeTexto = getExtraValue(xmlDoc, "qrCode");

        container.add(new Paragraph("\nConsulte pela Chave de Acesso em:\n"
                + firstNonBlank(urlConsulta, "www.nfe.fazenda.gov.br"))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(7));
        container.add(new Paragraph(formatarChave(chave))
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(8));

        Table footerTab = new Table(UnitValue.createPercentArray(new float[] { 40, 60 }))
                .useAllAvailableWidth()
                .setMarginTop(10);

        if (StringUtils.hasText(qrCodeTexto)) {
            try {
                BarcodeQRCode qrCode = new BarcodeQRCode(qrCodeTexto);
                Image qrImage = new Image(qrCode.createFormXObject(ColorConstants.BLACK, pdfDoc)).setAutoScale(true);
                footerTab.addCell(new Cell()
                        .add(qrImage)
                        .setBorder(Border.NO_BORDER)
                        .setPaddingLeft(20));
            } catch (Exception e) {
                footerTab.addCell(new Cell()
                        .add(new Paragraph("[ERRO QR]").setFontSize(7))
                        .setBorder(Border.NO_BORDER));
            }
        } else {
            footerTab.addCell(new Cell().setBorder(Border.NO_BORDER));
        }

        String consumidor = "CONSUMIDOR NAO IDENTIFICADO";
        if (dest != null) {
            String idConsumidor = firstNonBlank(getElementValue(dest, "CPF"), getElementValue(dest, "CNPJ"));
            if (StringUtils.hasText(idConsumidor)) {
                consumidor = "CONSUMIDOR CPF/CNPJ: " + formatarCnpjCpf(idConsumidor);
                String nomeConsumidor = getElementValue(dest, "xNome");
                if (StringUtils.hasText(nomeConsumidor)) {
                    consumidor = consumidor + "\n" + nomeConsumidor;
                }
            }
        }

        String infoNf = String.format(Locale.ROOT,
                "%s%n%nNFC-e n %s Serie %s%nData Emissao: %s",
                consumidor,
                getElementValue(ide, "nNF"),
                getElementValue(ide, "serie"),
                formatarData(firstNonBlank(getElementValue(ide, "dhEmi"), getElementValue(ide, "dEmi"))));

        footerTab.addCell(new Cell()
                .add(new Paragraph(infoNf).setFontSize(7))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
        container.add(footerTab);

        Element infAdic = getFirstElement(infNFe, "infAdic");
        if (infAdic != null) {
            String observacao = getElementValue(infAdic, "infCpl");
            if (StringUtils.hasText(observacao)) {
                container.add(new Paragraph("\nINFORMACOES ADICIONAIS").setBold().setFontSize(7));
                container.add(new Paragraph(observacao).setFontSize(6));
            }
        }

        if ("2".equals(getElementValue(ide, "tpAmb"))) {
            container.add(new Paragraph("\nAMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL")
                    .setFontColor(ColorConstants.RED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(9));
        }

        mainTable.addCell(container);
        document.add(mainTable);
        document.close();
        return baos.toByteArray();
    }

    private Cell bodyCell(String value, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(value).setFontSize(7))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(alignment);
    }

    private Paragraph separator() {
        return new Paragraph("-".repeat(70))
                .setFontSize(6)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private String getExtraValue(org.w3c.dom.Document document, String tagName) {
        NodeList nodeList = document.getElementsByTagNameNS("*", tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent().trim() : "";
    }

    private String getElementValue(Element element, String tagName) {
        if (element == null) {
            return "";
        }
        NodeList nodeList = element.getElementsByTagNameNS("*", tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent().trim() : "";
    }

    private Table criarLinhaTotal(String label, String valor) {
        return criarLinhaTotal(label, valor, false);
    }

    private Table criarLinhaTotal(String label, String valor, boolean bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1 })).useAllAvailableWidth();
        Paragraph left = new Paragraph(label).setFontSize(bold ? 8 : 7);
        Paragraph right = new Paragraph(firstNonBlank(valor, "0.00"))
                .setFontSize(bold ? 8 : 7)
                .setTextAlignment(TextAlignment.RIGHT);
        if (bold) {
            left.setBold();
            right.setBold();
        }
        table.addCell(new Cell().add(left).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(right).setBorder(Border.NO_BORDER));
        return table;
    }

    private String formatarChave(String chave) {
        return StringUtils.hasText(chave) ? chave.replaceAll("(.{4})", "$1 ").trim() : "";
    }

    private String formatarCnpjCpf(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\D", "");
        if (normalized.length() == 11) {
            return normalized.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (normalized.length() == 14) {
            return normalized.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return normalized;
    }

    private String formatarData(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            String normalized = value.trim();
            String base = normalized.length() >= 19 ? normalized.substring(0, 19) : normalized;
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ROOT)
                    .format(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).parse(base));
        } catch (Exception e) {
            return value;
        }
    }

    private org.w3c.dom.Document parseXmlString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private Element requireElement(org.w3c.dom.Document document, String tagName) {
        Element element = getFirstElement(document.getDocumentElement(), tagName);
        if (element == null) {
            throw new IllegalArgumentException("Tag obrigatoria nao encontrada no XML NFC-e: " + tagName);
        }
        return element;
    }

    private Element requireChild(Element parent, String tagName) {
        Element element = getFirstElement(parent, tagName);
        if (element == null) {
            throw new IllegalArgumentException("Tag obrigatoria nao encontrada no XML NFC-e: " + tagName);
        }
        return element;
    }

    private Element getFirstElement(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList nodeList = parent.getElementsByTagNameNS(NFE_NAMESPACE, tagName);
        if (nodeList.getLength() == 0) {
            nodeList = parent.getElementsByTagNameNS("*", tagName);
        }
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    private boolean isPositiveNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.')) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private String getFormaPagamento(String codigo) {
        return switch (codigo) {
            case "01" -> "Dinheiro";
            case "02" -> "Cheque";
            case "03" -> "Cartao de Credito";
            case "04" -> "Cartao de Debito";
            case "05" -> "Credito Loja";
            case "10" -> "Vale Alimentacao";
            case "11" -> "Vale Refeicao";
            case "12" -> "Vale Presente";
            case "13" -> "Vale Combustivel";
            case "14" -> "Duplicata Mercantil";
            case "15" -> "Boleto Bancario";
            case "17" -> "PIX";
            case "90" -> "Sem pagamento";
            default -> "Outros";
        };
    }
}
