package isakids.nfcelab.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Component
public class DocumentoFiscalXmlParser {

    public Optional<DocumentoFiscalResumo> parse(String xml) {
        if (!StringUtils.hasText(xml)) {
            return Optional.empty();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            XPath xpath = XPathFactory.newInstance().newXPath();

            String chaveAcesso = trimToNull(firstNonBlank(
                    evaluate(xpath, document, "string(//*[local-name()='infProt']/*[local-name()='chNFe'])"),
                    normalizeInfNfeId(evaluate(xpath, document, "string(//*[local-name()='infNFe']/@Id)"))));

            String numeroNota = trimToNull(evaluate(xpath, document, "string(//*[local-name()='ide']/*[local-name()='nNF'])"));
            String serie = trimToNull(evaluate(xpath, document, "string(//*[local-name()='ide']/*[local-name()='serie'])"));
            LocalDateTime dataEmissao = parseDateTime(firstNonBlank(
                    evaluate(xpath, document, "string(//*[local-name()='ide']/*[local-name()='dhEmi'])"),
                    evaluate(xpath, document, "string(//*[local-name()='ide']/*[local-name()='dEmi'])")));
            String emitenteCnpj = digitsOrNull(evaluate(xpath, document, "string(//*[local-name()='emit']/*[local-name()='CNPJ'])"));
            String destinatarioNome = trimToNull(evaluate(xpath, document, "string(//*[local-name()='dest']/*[local-name()='xNome'])"));
            String destinatarioDocumento = digitsOrNull(firstNonBlank(
                    evaluate(xpath, document, "string(//*[local-name()='dest']/*[local-name()='CNPJ'])"),
                    evaluate(xpath, document, "string(//*[local-name()='dest']/*[local-name()='CPF'])")));
            BigDecimal valorTotal = parseDecimal(evaluate(xpath, document, "string(//*[local-name()='ICMSTot']/*[local-name()='vNF'])"));
            String observacao = trimToNull(evaluate(xpath, document, "string(//*[local-name()='infAdic']/*[local-name()='infCpl'])"));
            String protocoloAutorizacao = trimToNull(firstNonBlank(
                    evaluate(xpath, document, "string(//*[local-name()='infProt']/*[local-name()='nProt'])"),
                    evaluate(xpath, document, "string(//*[local-name()='protNFe']//*[local-name()='nProt'])")));

            return Optional.of(new DocumentoFiscalResumo(
                    chaveAcesso,
                    numeroNota,
                    serie,
                    dataEmissao,
                    emitenteCnpj,
                    destinatarioNome,
                    destinatarioDocumento,
                    valorTotal,
                    observacao,
                    protocoloAutorizacao));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String evaluate(XPath xpath, Document document, String expression) throws Exception {
        return (String) xpath.evaluate(expression, document, XPathConstants.STRING);
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(trimmed);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(trimmed).atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        try {
            return new BigDecimal(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeInfNfeId(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.startsWith("NFe") ? trimmed.substring(3) : trimmed;
    }

    private String digitsOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed.replaceAll("\\D", "") : null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record DocumentoFiscalResumo(
            String chaveAcesso,
            String numeroNota,
            String serie,
            LocalDateTime dataEmissao,
            String emitenteCnpj,
            String destinatarioNome,
            String destinatarioDocumento,
            BigDecimal valorTotal,
            String observacao,
            String protocoloAutorizacao) {
    }
}
