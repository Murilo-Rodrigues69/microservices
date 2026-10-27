package isakids.nfcelab.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class NfcePdfRenderer {

    private static final float MARGIN = 40f;
    private static final float BODY_FONT_SIZE = 10f;
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float LEADING = 14f;

    public byte[] render(String xml) {
        try (PDDocument pdf = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ParsedDocument parsed = parse(xml);
            PdfWriter writer = new PdfWriter(pdf);

            writer.writeTitle(parsed.title());
            writer.writeBlankLine();
            writer.writeLabelValue("Chave de acesso", parsed.chaveAcesso());
            writer.writeLabelValue("Numero", parsed.numeroNota());
            writer.writeLabelValue("Serie", parsed.serie());
            writer.writeLabelValue("Data/hora emissao", parsed.dataHoraEmissao());
            writer.writeLabelValue("Protocolo", parsed.protocolo());
            writer.writeLabelValue("Status", parsed.statusLinha());
            writer.writeBlankLine();

            writer.writeSection("Emitente");
            writer.writeParagraph(parsed.emitenteNome());
            writer.writeParagraph(parsed.emitenteDocumentoLinha());
            writer.writeParagraph(parsed.emitenteEndereco());
            writer.writeBlankLine();

            writer.writeSection("Destinatario");
            writer.writeParagraph(parsed.destinatarioNome());
            writer.writeParagraph(parsed.destinatarioDocumentoLinha());
            writer.writeParagraph(parsed.destinatarioEndereco());
            writer.writeBlankLine();

            writer.writeSection("Itens");
            if (parsed.itens().isEmpty()) {
                writer.writeParagraph("Nenhum item encontrado no XML.");
            } else {
                int index = 1;
                for (String item : parsed.itens()) {
                    writer.writeParagraph(index++ + ". " + item);
                }
            }
            writer.writeBlankLine();

            writer.writeSection("Totais");
            writer.writeLabelValue("Valor produtos", parsed.valorProdutos());
            writer.writeLabelValue("Desconto", parsed.valorDesconto());
            writer.writeLabelValue("Valor nota", parsed.valorNota());

            if (StringUtils.hasText(parsed.qrCodeUrl())) {
                writer.writeBlankLine();
                writer.writeSection("QR Code / consulta");
                writer.writeParagraph(parsed.qrCodeUrl());
            }

            writer.close();
            pdf.save(output);
            return output.toByteArray();
        } catch (Exception e) {
            return renderFallback(xml, e);
        }
    }

    private byte[] renderFallback(String xml, Exception cause) {
        try (PDDocument pdf = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(pdf);
            writer.writeTitle("Documento fiscal em PDF");
            writer.writeBlankLine();
            writer.writeParagraph("Nao foi possivel montar a representacao estruturada do XML.");
            writer.writeParagraph("Motivo: " + cause.getMessage());
            writer.writeBlankLine();
            writer.writeSection("Conteudo XML");
            for (String line : splitXmlForPdf(xml)) {
                writer.writeParagraph(line);
            }
            writer.close();
            pdf.save(output);
            return output.toByteArray();
        } catch (Exception pdfException) {
            throw new IllegalStateException("Falha gerando PDF do documento fiscal.", pdfException);
        }
    }

    private List<String> splitXmlForPdf(String xml) {
        String normalized = StringUtils.hasText(xml) ? xml.replaceAll(">\\s+<", ">\n<") : "";
        String[] lines = normalized.split("\\R");
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }

    private ParsedDocument parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        var xpath = XPathFactory.newInstance().newXPath();

        String modelo = eval(xpath, document, "string(//*[local-name()='ide']/*[local-name()='mod'])");
        String chaveAcesso = eval(xpath, document, "string((//*[local-name()='infNFe']/@Id)[1])");
        if (StringUtils.hasText(chaveAcesso) && chaveAcesso.startsWith("NFe")) {
            chaveAcesso = chaveAcesso.substring(3);
        }

        List<String> itens = new ArrayList<>();
        NodeList dets = (NodeList) xpath.evaluate("//*[local-name()='det']", document, XPathConstants.NODESET);
        for (int i = 0; i < dets.getLength(); i++) {
            Node det = dets.item(i);
            String descricao = eval(xpath, det, "string(./*[local-name()='prod']/*[local-name()='xProd'])");
            String quantidade = eval(xpath, det, "string(./*[local-name()='prod']/*[local-name()='qCom'])");
            String unidade = eval(xpath, det, "string(./*[local-name()='prod']/*[local-name()='uCom'])");
            String valorUnitario = eval(xpath, det, "string(./*[local-name()='prod']/*[local-name()='vUnCom'])");
            String valorTotal = eval(xpath, det, "string(./*[local-name()='prod']/*[local-name()='vProd'])");
            String ncm = eval(xpath, det, "string(./*[local-name()='prod']/*[local-name()='NCM'])");
            itens.add(firstNonBlank(descricao, "ITEM")
                    + " | qtd " + firstNonBlank(quantidade, "-")
                    + " " + firstNonBlank(unidade, "")
                    + " | unit " + firstNonBlank(valorUnitario, "-")
                    + " | total " + firstNonBlank(valorTotal, "-")
                    + " | ncm " + firstNonBlank(ncm, "-"));
        }

        String emitenteDocumento = firstNonBlank(
                eval(xpath, document, "string(//*[local-name()='emit']/*[local-name()='CNPJ'])"),
                eval(xpath, document, "string(//*[local-name()='emit']/*[local-name()='CPF'])"));
        String destinatarioDocumento = firstNonBlank(
                eval(xpath, document, "string(//*[local-name()='dest']/*[local-name()='CNPJ'])"),
                eval(xpath, document, "string(//*[local-name()='dest']/*[local-name()='CPF'])"));

        String emitenteDocumentoLinha = joinParts(
                "Documento: " + emitenteDocumento,
                prefixedIfPresent("IE: ", eval(xpath, document, "string(//*[local-name()='emit']/*[local-name()='IE'])")));
        String destinatarioDocumentoLinha = StringUtils.hasText(destinatarioDocumento)
                ? "Documento: " + destinatarioDocumento
                : "Documento nao informado.";

        return new ParsedDocument(
                "65".equals(modelo) ? "NFC-e autorizada" : "NF-e autorizada",
                chaveAcesso,
                eval(xpath, document, "string(//*[local-name()='ide']/*[local-name()='nNF'])"),
                eval(xpath, document, "string(//*[local-name()='ide']/*[local-name()='serie'])"),
                firstNonBlank(
                        eval(xpath, document, "string(//*[local-name()='ide']/*[local-name()='dhEmi'])"),
                        eval(xpath, document, "string(//*[local-name()='ide']/*[local-name()='dEmi'])")),
                eval(xpath, document, "string(//*[local-name()='infProt']/*[local-name()='nProt'])"),
                joinParts(
                        prefixedIfPresent("cStat ", eval(xpath, document, "string(//*[local-name()='infProt']/*[local-name()='cStat'])")),
                        eval(xpath, document, "string(//*[local-name()='infProt']/*[local-name()='xMotivo'])")),
                firstNonBlank(
                        eval(xpath, document, "string(//*[local-name()='emit']/*[local-name()='xFant'])"),
                        eval(xpath, document, "string(//*[local-name()='emit']/*[local-name()='xNome'])")),
                emitenteDocumentoLinha,
                joinParts(
                        joinParts(
                                eval(xpath, document, "string(//*[local-name()='enderEmit']/*[local-name()='xLgr'])"),
                                eval(xpath, document, "string(//*[local-name()='enderEmit']/*[local-name()='nro'])")),
                        eval(xpath, document, "string(//*[local-name()='enderEmit']/*[local-name()='xBairro'])"),
                        joinParts(
                                eval(xpath, document, "string(//*[local-name()='enderEmit']/*[local-name()='xMun'])"),
                                eval(xpath, document, "string(//*[local-name()='enderEmit']/*[local-name()='UF'])")),
                        prefixedIfPresent("CEP ", eval(xpath, document, "string(//*[local-name()='enderEmit']/*[local-name()='CEP'])"))),
                firstNonBlank(
                        eval(xpath, document, "string(//*[local-name()='dest']/*[local-name()='xNome'])"),
                        "Nao informado"),
                destinatarioDocumentoLinha,
                joinParts(
                        joinParts(
                                eval(xpath, document, "string(//*[local-name()='enderDest']/*[local-name()='xLgr'])"),
                                eval(xpath, document, "string(//*[local-name()='enderDest']/*[local-name()='nro'])")),
                        eval(xpath, document, "string(//*[local-name()='enderDest']/*[local-name()='xBairro'])"),
                        joinParts(
                                eval(xpath, document, "string(//*[local-name()='enderDest']/*[local-name()='xMun'])"),
                                eval(xpath, document, "string(//*[local-name()='enderDest']/*[local-name()='UF'])")),
                        prefixedIfPresent("CEP ", eval(xpath, document, "string(//*[local-name()='enderDest']/*[local-name()='CEP'])"))),
                eval(xpath, document, "string(//*[local-name()='ICMSTot']/*[local-name()='vProd'])"),
                eval(xpath, document, "string(//*[local-name()='ICMSTot']/*[local-name()='vDesc'])"),
                eval(xpath, document, "string(//*[local-name()='ICMSTot']/*[local-name()='vNF'])"),
                eval(xpath, document, "string(//*[local-name()='infNFeSupl']/*[local-name()='qrCode'])"),
                itens);
    }

    private String prefixedIfPresent(String prefix, String value) {
        return StringUtils.hasText(value) ? prefix + value.trim() : null;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private String joinParts(String... parts) {
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                values.add(part.trim());
            }
        }
        return values.isEmpty() ? "Nao informado" : String.join(" | ", values);
    }

    private String eval(javax.xml.xpath.XPath xpath, Object item, String expression) throws Exception {
        String value = xpath.evaluate(expression, item);
        return value != null ? value.trim() : null;
    }

    private record ParsedDocument(
            String title,
            String chaveAcesso,
            String numeroNota,
            String serie,
            String dataHoraEmissao,
            String protocolo,
            String statusLinha,
            String emitenteNome,
            String emitenteDocumentoLinha,
            String emitenteEndereco,
            String destinatarioNome,
            String destinatarioDocumentoLinha,
            String destinatarioEndereco,
            String valorProdutos,
            String valorDesconto,
            String valorNota,
            String qrCodeUrl,
            List<String> itens) {
    }

    private static class PdfWriter {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private PdfWriter(PDDocument document) throws Exception {
            this.document = document;
            newPage();
        }

        private void writeTitle(String text) throws Exception {
            writeWrapped(text, PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
        }

        private void writeSection(String text) throws Exception {
            writeWrapped(text, PDType1Font.HELVETICA_BOLD, 12f);
        }

        private void writeLabelValue(String label, String value) throws Exception {
            writeWrapped(label + ": " + firstNonBlank(value), PDType1Font.HELVETICA, BODY_FONT_SIZE);
        }

        private void writeParagraph(String text) throws Exception {
            writeWrapped(firstNonBlank(text), PDType1Font.HELVETICA, BODY_FONT_SIZE);
        }

        private void writeBlankLine() throws Exception {
            ensureSpace(LEADING);
            y -= LEADING;
        }

        private void writeWrapped(String text, PDType1Font font, float fontSize) throws Exception {
            for (String line : wrap(text, font, fontSize)) {
                ensureSpace(LEADING);
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(MARGIN, y);
                contentStream.showText(line);
                contentStream.endText();
                y -= LEADING;
            }
        }

        private List<String> wrap(String text, PDType1Font font, float fontSize) throws Exception {
            List<String> lines = new ArrayList<>();
            String safeText = firstNonBlank(text);
            float maxWidth = page.getMediaBox().getWidth() - (MARGIN * 2);
            String[] words = safeText.replace('\n', ' ').split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void ensureSpace(float requiredHeight) throws Exception {
            if (y - requiredHeight <= MARGIN) {
                newPage();
            }
        }

        private void newPage() throws Exception {
            if (contentStream != null) {
                contentStream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void close() throws Exception {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private String firstNonBlank(String value) {
            return StringUtils.hasText(value) ? value.trim() : "-";
        }
    }
}
