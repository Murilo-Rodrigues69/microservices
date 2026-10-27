package isakids.nfcelab.document;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;

import isakids.nfcelab.config.NfceDocumentProperties;
import isakids.nfcelab.config.NfceDocumentProperties.PdfProvider;
import isakids.nfcelab.entity.NotaFiscalDocumentoEntity;
import isakids.nfcelab.entity.NotaFiscalEmpresaEntity;
import isakids.nfcelab.fiscal.NfceEmissionResult;
import isakids.nfcelab.fiscal.NfceEmitter;
import isakids.nfcelab.repository.NotaFiscalDocumentoRepository;
import isakids.nfcelab.repository.NotaFiscalEmpresaRepository;

@Service
public class NfceDocumentService {

    private static final Logger log = LoggerFactory.getLogger(NfceDocumentService.class);
    private static final String NFE_NAMESPACE = "http://www.portalfiscal.inf.br/nfe";

    private final NfceDocumentProperties properties;
    private final NfcePdfRenderer pdfRenderer;
    private final NfceDanfeRenderer nfceDanfeRenderer;
    private final MeuDanfeClient meuDanfeClient;
    private final NotaFiscalDocumentoRepository notaFiscalDocumentoRepository;
    private final NotaFiscalEmpresaRepository notaFiscalEmpresaRepository;
    private final NfceEmitter emitter;
    private final FiscalDocumentStorage remoteStorage;

    public NfceDocumentService(NfceDocumentProperties properties, NfcePdfRenderer pdfRenderer,
            NfceDanfeRenderer nfceDanfeRenderer, MeuDanfeClient meuDanfeClient) {
        this(properties, pdfRenderer, nfceDanfeRenderer, meuDanfeClient, null, null, null, null);
    }

    @Autowired
    public NfceDocumentService(NfceDocumentProperties properties, NfcePdfRenderer pdfRenderer,
            NfceDanfeRenderer nfceDanfeRenderer, MeuDanfeClient meuDanfeClient,
            NotaFiscalDocumentoRepository notaFiscalDocumentoRepository,
            NotaFiscalEmpresaRepository notaFiscalEmpresaRepository,
            NfceEmitter emitter,
            FiscalDocumentStorage remoteStorage) {
        this.properties = properties;
        this.pdfRenderer = pdfRenderer;
        this.nfceDanfeRenderer = nfceDanfeRenderer;
        this.meuDanfeClient = meuDanfeClient;
        this.notaFiscalDocumentoRepository = notaFiscalDocumentoRepository;
        this.notaFiscalEmpresaRepository = notaFiscalEmpresaRepository;
        this.emitter = emitter;
        this.remoteStorage = remoteStorage;
    }

    public NfceDocumentArtifacts store(String chaveAcesso, String xml) {
        GeneratedPdf generatedPdf = generatePdf(xml);
        return store(chaveAcesso, xml, generatedPdf.pdfBytes(), generatedPdf.source());
    }

    public NfceDocumentArtifacts store(String chaveAcesso, String xml, byte[] pdfBytes) {
        return store(chaveAcesso, xml, pdfBytes, PdfSource.LOCAL);
    }

    public NfceDocumentArtifacts store(String chaveAcesso, String xml, byte[] pdfBytes, PdfSource pdfSource) {
        String normalizedChave = normalizeChave(chaveAcesso);
        persistDocument(normalizedChave, xml, pdfBytes);
        storeRemoteDocument(normalizedChave, xml, pdfBytes);
        writeLocalCache(normalizedChave, xml, pdfBytes, pdfSource);
        return new NfceDocumentArtifacts(resolveXmlPath(normalizedChave).toAbsolutePath().normalize(),
                resolvePdfPath(normalizedChave).toAbsolutePath().normalize());
    }

    public Optional<NfceDocumentArtifacts> find(String chaveAcesso) {
        String normalizedChave = normalizeChave(chaveAcesso);

        Optional<NfceStoredDocument> remoteDocument = loadRemoteDocument(normalizedChave);
        if (remoteDocument.isPresent()) {
            StoredDocumentResult result = maybeUpgradeStoredDocument(normalizedChave, remoteDocument.get());
            persistDocument(normalizedChave, result.document().xml(), result.document().pdfBytes());
            writeLocalCache(normalizedChave, result.document().xml(), result.document().pdfBytes(), result.source());
            return Optional.of(new NfceDocumentArtifacts(resolveXmlPath(normalizedChave).toAbsolutePath().normalize(),
                    resolvePdfPath(normalizedChave).toAbsolutePath().normalize()));
        }

        Optional<NfceStoredDocument> persistedDocument = loadPersistedDocument(normalizedChave);
        if (persistedDocument.isPresent()) {
            StoredDocumentResult result = maybeUpgradeStoredDocument(normalizedChave, persistedDocument.get());
            storeRemoteDocument(normalizedChave, result.document().xml(), result.document().pdfBytes());
            writeLocalCache(normalizedChave, result.document().xml(), result.document().pdfBytes(), result.source());
            return Optional.of(new NfceDocumentArtifacts(resolveXmlPath(normalizedChave).toAbsolutePath().normalize(),
                    resolvePdfPath(normalizedChave).toAbsolutePath().normalize()));
        }

        Path xmlPath = resolveXmlPath(normalizedChave);
        if (Files.exists(xmlPath)) {
            ensureLocalPdfUpToDate(normalizedChave, xmlPath);
            backfillDatabaseFromLocalCache(normalizedChave, xmlPath, resolvePdfPath(normalizedChave));
            backfillRemoteFromLocalCache(normalizedChave, xmlPath, resolvePdfPath(normalizedChave));
            return Optional.of(new NfceDocumentArtifacts(xmlPath.toAbsolutePath().normalize(),
                    resolvePdfPath(normalizedChave).toAbsolutePath().normalize()));
        }

        Optional<NfceStoredDocument> recoveredDocument = recoverStoredDocument(normalizedChave);
        if (recoveredDocument.isPresent()) {
            writeLocalCache(normalizedChave, recoveredDocument.get().xml(), recoveredDocument.get().pdfBytes(), PdfSource.LOCAL);
            return Optional.of(new NfceDocumentArtifacts(resolveXmlPath(normalizedChave).toAbsolutePath().normalize(),
                    resolvePdfPath(normalizedChave).toAbsolutePath().normalize()));
        }

        return Optional.empty();
    }

    public String loadXml(String chaveAcesso) {
        byte[] xmlBytes = loadXmlBytes(chaveAcesso);
        return xmlBytes != null ? new String(xmlBytes, StandardCharsets.UTF_8) : null;
    }

    public byte[] loadPdf(String chaveAcesso) {
        String normalizedChave = normalizeChave(chaveAcesso);
        Optional<NfceStoredDocument> persistedDocument = loadPersistedDocument(normalizedChave);
        if (persistedDocument.isPresent()) {
            StoredDocumentResult result = maybeUpgradeStoredDocument(normalizedChave, persistedDocument.get());
            storeRemoteDocument(normalizedChave, result.document().xml(), result.document().pdfBytes());
            writeLocalCache(normalizedChave, result.document().xml(), result.document().pdfBytes(), result.source());
            return result.document().pdfBytes();
        }

        try {
            Optional<NfceDocumentArtifacts> artifacts = find(normalizedChave);
            if (artifacts.isEmpty()) {
                return null;
            }
            return Files.readAllBytes(artifacts.get().pdfPath());
        } catch (IOException e) {
            throw new IllegalStateException("Falha lendo PDF da nota " + normalizedChave + ".", e);
        }
    }

    public byte[] loadXmlBytes(String chaveAcesso) {
        String normalizedChave = normalizeChave(chaveAcesso);
        Optional<NfceStoredDocument> persistedDocument = loadPersistedDocument(normalizedChave);
        if (persistedDocument.isPresent()) {
            storeRemoteDocument(normalizedChave, persistedDocument.get().xml(), persistedDocument.get().pdfBytes());
            writeLocalCache(normalizedChave, persistedDocument.get().xml(), persistedDocument.get().pdfBytes(), PdfSource.LOCAL);
            return persistedDocument.get().xml().getBytes(StandardCharsets.UTF_8);
        }

        try {
            Optional<NfceDocumentArtifacts> artifacts = find(normalizedChave);
            if (artifacts.isEmpty()) {
                return null;
            }
            return Files.readAllBytes(artifacts.get().xmlPath());
        } catch (IOException e) {
            throw new IllegalStateException("Falha lendo arquivo XML da nota " + normalizedChave + ".", e);
        }
    }

    private void ensureLocalPdfUpToDate(String chaveAcesso, Path xmlPath) {
        Path pdfPath = resolvePdfPath(chaveAcesso);
        Path pdfSourcePath = resolvePdfSourcePath(chaveAcesso);
        if (!shouldRefreshPdf(pdfPath, pdfSourcePath, xmlPath)) {
            return;
        }

        try {
            GeneratedPdf generatedPdf = generatePdf(Files.readString(xmlPath, StandardCharsets.UTF_8));
            Files.write(pdfPath, generatedPdf.pdfBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(pdfSourcePath, generatedPdf.source().name(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Falha regenerando PDF da nota " + chaveAcesso + ".", e);
        }
    }

    private void backfillDatabaseFromLocalCache(String chaveAcesso, Path xmlPath, Path pdfPath) {
        if (notaFiscalDocumentoRepository == null || notaFiscalDocumentoRepository.findByChaveAcesso(chaveAcesso).isPresent()) {
            return;
        }

        try {
            if (!Files.exists(pdfPath)) {
                return;
            }
            persistDocument(chaveAcesso,
                    Files.readString(xmlPath, StandardCharsets.UTF_8),
                    Files.readAllBytes(pdfPath));
        } catch (IOException e) {
            log.warn("Falha ao sincronizar documento local da nota {} para o banco.", chaveAcesso, e);
        }
    }

    private void backfillRemoteFromLocalCache(String chaveAcesso, Path xmlPath, Path pdfPath) {
        if (remoteStorage == null || !remoteStorage.isEnabled()) {
            return;
        }

        try {
            if (!Files.exists(pdfPath)) {
                return;
            }
            storeRemoteDocument(chaveAcesso,
                    Files.readString(xmlPath, StandardCharsets.UTF_8),
                    Files.readAllBytes(pdfPath));
        } catch (IOException e) {
            log.warn("Falha ao sincronizar documento local da nota {} para o R2.", chaveAcesso, e);
        }
    }

    private Optional<NfceStoredDocument> recoverStoredDocument(String chaveAcesso) {
        Optional<NfceStoredDocument> meuDanfeDocument = fetchStoredDocumentFromMeuDanfe(chaveAcesso);
        if (meuDanfeDocument.isPresent()) {
            return meuDanfeDocument;
        }
        return recoverStoredDocumentFromRegisteredNote(chaveAcesso);
    }

    private Optional<NfceStoredDocument> fetchStoredDocumentFromMeuDanfe(String chaveAcesso) {
        if (!shouldPreferMeuDanfeForChave(chaveAcesso)) {
            return Optional.empty();
        }

        try {
            return meuDanfeClient.baixarDocumentosPorChave(chaveAcesso)
                    .map(document -> persistAndReturn(chaveAcesso, document.xml(), document.pdf(), PdfSource.MEUDANFE));
        } catch (Exception e) {
            log.warn("Falha ao recuperar documentos da nota {} via Meu Danfe.", chaveAcesso, e);
            return Optional.empty();
        }
    }

    private Optional<NfceStoredDocument> recoverStoredDocumentFromRegisteredNote(String chaveAcesso) {
        if (notaFiscalEmpresaRepository == null || emitter == null) {
            return Optional.empty();
        }

        Optional<NotaFiscalEmpresaEntity> nota = notaFiscalEmpresaRepository.findByChaveAcessoWithEmpresa(chaveAcesso);
        if (nota.isEmpty()) {
            return Optional.empty();
        }

        try {
            NfceEmissionResult consulta = emitter.consultarPorChave(nota.get().getEmpresa(), chaveAcesso);
            if (!StringUtils.hasText(consulta.getXmlAutorizado())) {
                return Optional.empty();
            }

            GeneratedPdf generatedPdf = generatePdf(consulta.getXmlAutorizado());
            return Optional.of(persistAndReturn(chaveAcesso, consulta.getXmlAutorizado(), generatedPdf.pdfBytes(),
                    generatedPdf.source()));
        } catch (Exception e) {
            log.warn("Falha ao recuperar documentos da nota {} via consulta SEFAZ.", chaveAcesso, e);
            return Optional.empty();
        }
    }

    private NfceStoredDocument persistAndReturn(String chaveAcesso, String xml, byte[] pdfBytes, PdfSource pdfSource) {
        persistDocument(chaveAcesso, xml, pdfBytes);
        storeRemoteDocument(chaveAcesso, xml, pdfBytes);
        writeLocalCache(chaveAcesso, xml, pdfBytes, pdfSource);
        return new NfceStoredDocument(xml, pdfBytes);
    }

    private void persistDocument(String chaveAcesso, String xml, byte[] pdfBytes) {
        if (notaFiscalDocumentoRepository == null) {
            return;
        }

        NotaFiscalDocumentoEntity documento = notaFiscalDocumentoRepository.findByChaveAcesso(chaveAcesso)
                .orElseGet(NotaFiscalDocumentoEntity::new);
        documento.setChaveAcesso(chaveAcesso);
        documento.setXmlConteudo(xml);
        documento.setPdfBase64(Base64.getEncoder().encodeToString(pdfBytes));
        notaFiscalDocumentoRepository.save(documento);
    }

    private Optional<NfceStoredDocument> loadPersistedDocument(String chaveAcesso) {
        if (notaFiscalDocumentoRepository == null) {
            return Optional.empty();
        }

        return notaFiscalDocumentoRepository.findByChaveAcesso(chaveAcesso)
                .map(documento -> new NfceStoredDocument(
                        documento.getXmlConteudo(),
                        Base64.getDecoder().decode(documento.getPdfBase64())));
    }

    private StoredDocumentResult maybeUpgradeStoredDocument(String chaveAcesso, NfceStoredDocument document) {
        if (!shouldPreferMeuDanfe(document.xml())) {
            return new StoredDocumentResult(document, PdfSource.LOCAL);
        }

        Optional<PdfSource> currentSource = readPdfSource(resolvePdfSourcePath(chaveAcesso));
        if (currentSource.orElse(PdfSource.LOCAL) == PdfSource.MEUDANFE) {
            return new StoredDocumentResult(document, PdfSource.MEUDANFE);
        }

        GeneratedPdf generatedPdf = generatePdf(document.xml());
        if (generatedPdf.source() != PdfSource.MEUDANFE) {
            return new StoredDocumentResult(document, currentSource.orElse(PdfSource.LOCAL));
        }

        persistDocument(chaveAcesso, document.xml(), generatedPdf.pdfBytes());
        storeRemoteDocument(chaveAcesso, document.xml(), generatedPdf.pdfBytes());
        writeLocalCache(chaveAcesso, document.xml(), generatedPdf.pdfBytes(), generatedPdf.source());
        return new StoredDocumentResult(new NfceStoredDocument(document.xml(), generatedPdf.pdfBytes()),
                generatedPdf.source());
    }

    private Optional<NfceStoredDocument> loadRemoteDocument(String chaveAcesso) {
        if (remoteStorage == null || !remoteStorage.isEnabled()) {
            return Optional.empty();
        }

        try {
            return remoteStorage.load(chaveAcesso);
        } catch (Exception e) {
            log.warn("Falha ao carregar documentos da nota {} no R2.", chaveAcesso, e);
            return Optional.empty();
        }
    }

    private void storeRemoteDocument(String chaveAcesso, String xml, byte[] pdfBytes) {
        if (remoteStorage == null || !remoteStorage.isEnabled()) {
            return;
        }

        try {
            remoteStorage.store(chaveAcesso, xml, pdfBytes);
        } catch (Exception e) {
            log.warn("Falha ao enviar documentos da nota {} para o R2.", chaveAcesso, e);
        }
    }

    private void writeLocalCache(String chaveAcesso, String xml, byte[] pdfBytes, PdfSource pdfSource) {
        try {
            Path xmlPath = resolveXmlPath(chaveAcesso);
            Path pdfPath = resolvePdfPath(chaveAcesso);
            Path pdfSourcePath = resolvePdfSourcePath(chaveAcesso);
            Files.createDirectories(xmlPath.getParent());
            Files.writeString(xmlPath, xml, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(pdfPath, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(pdfSourcePath, pdfSource.name(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("Falha ao atualizar cache local dos documentos da nota {}.", chaveAcesso, e);
        }
    }

    private Path resolveXmlPath(String chaveAcesso) {
        return resolveBaseDir().resolve(normalizeChave(chaveAcesso) + ".xml");
    }

    private Path resolvePdfPath(String chaveAcesso) {
        return resolveBaseDir().resolve(normalizeChave(chaveAcesso) + ".pdf");
    }

    private Path resolvePdfSourcePath(String chaveAcesso) {
        return resolveBaseDir().resolve(normalizeChave(chaveAcesso) + ".pdf.source");
    }

    private Path resolveBaseDir() {
        String outputDir = StringUtils.hasText(properties.getOutputDir()) ? properties.getOutputDir().trim()
                : "documentos-fiscais";
        return Paths.get(outputDir);
    }

    private String normalizeChave(String chaveAcesso) {
        if (!StringUtils.hasText(chaveAcesso)) {
            throw new IllegalArgumentException("Chave de acesso nao informada.");
        }
        return chaveAcesso.replaceAll("\\D", "");
    }

    private boolean shouldRefreshPdf(Path pdfPath, Path pdfSourcePath, Path xmlPath) {
        if (!Files.exists(pdfPath)) {
            return true;
        }

        if (!shouldPreferMeuDanfe(xmlPath)) {
            return false;
        }

        return readPdfSource(pdfSourcePath).orElse(PdfSource.LOCAL) != PdfSource.MEUDANFE;
    }

    private boolean shouldPreferMeuDanfe(Path xmlPath) {
        try {
            return shouldPreferMeuDanfe(Files.readString(xmlPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Falha lendo XML para decidir a origem do PDF.", e);
        }
    }

    private boolean shouldPreferMeuDanfe(String xml) {
        if (resolveFiscalModel(xml) == FiscalDocumentModel.NFCE) {
            return false;
        }

        PdfProvider provider = properties.getPdfProvider() != null ? properties.getPdfProvider() : PdfProvider.AUTO;
        return switch (provider) {
            case LOCAL -> false;
            case MEUDANFE -> true;
            case AUTO -> meuDanfeClient.isConfigured();
        };
    }

    private Optional<PdfSource> readPdfSource(Path pdfSourcePath) {
        if (!Files.exists(pdfSourcePath)) {
            return Optional.empty();
        }

        try {
            String value = Files.readString(pdfSourcePath, StandardCharsets.UTF_8).trim();
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            return Optional.of(PdfSource.valueOf(value));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private GeneratedPdf generatePdf(String xml) {
        if (resolveFiscalModel(xml) == FiscalDocumentModel.NFCE) {
            try {
                return new GeneratedPdf(nfceDanfeRenderer.render(xml), PdfSource.LOCAL);
            } catch (Exception e) {
                return new GeneratedPdf(pdfRenderer.render(xml), PdfSource.LOCAL);
            }
        }

        PdfProvider provider = properties.getPdfProvider() != null ? properties.getPdfProvider() : PdfProvider.AUTO;
        return switch (provider) {
            case LOCAL -> new GeneratedPdf(pdfRenderer.render(xml), PdfSource.LOCAL);
            case MEUDANFE -> new GeneratedPdf(meuDanfeClient.gerarDanfePdf(xml), PdfSource.MEUDANFE);
            case AUTO -> generateAuto(xml);
        };
    }

    private GeneratedPdf generateAuto(String xml) {
        if (!meuDanfeClient.isConfigured()) {
            return new GeneratedPdf(pdfRenderer.render(xml), PdfSource.LOCAL);
        }

        try {
            return new GeneratedPdf(meuDanfeClient.gerarDanfePdf(xml), PdfSource.MEUDANFE);
        } catch (Exception e) {
            return new GeneratedPdf(pdfRenderer.render(xml), PdfSource.LOCAL);
        }
    }

    private boolean shouldPreferMeuDanfeForChave(String chaveAcesso) {
        if (resolveFiscalModelFromChave(chaveAcesso) == FiscalDocumentModel.NFCE) {
            return false;
        }
        return shouldPreferMeuDanfeForNfe();
    }

    private boolean shouldPreferMeuDanfeForNfe() {
        PdfProvider provider = properties.getPdfProvider() != null ? properties.getPdfProvider() : PdfProvider.AUTO;
        return switch (provider) {
            case LOCAL -> false;
            case MEUDANFE -> true;
            case AUTO -> meuDanfeClient.isConfigured();
        };
    }

    private FiscalDocumentModel resolveFiscalModel(String xml) {
        if (!StringUtils.hasText(xml)) {
            return FiscalDocumentModel.UNKNOWN;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            org.w3c.dom.Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            org.w3c.dom.NodeList nodeList = document.getElementsByTagNameNS(NFE_NAMESPACE, "mod");
            if (nodeList.getLength() == 0) {
                nodeList = document.getElementsByTagNameNS("*", "mod");
            }
            if (nodeList.getLength() == 0) {
                return FiscalDocumentModel.UNKNOWN;
            }
            return FiscalDocumentModel.fromCodigo(nodeList.item(0).getTextContent());
        } catch (Exception e) {
            return FiscalDocumentModel.UNKNOWN;
        }
    }

    private FiscalDocumentModel resolveFiscalModelFromChave(String chaveAcesso) {
        String normalized = normalizeChave(chaveAcesso);
        if (normalized.length() < 22) {
            return FiscalDocumentModel.UNKNOWN;
        }
        return FiscalDocumentModel.fromCodigo(normalized.substring(20, 22));
    }

    public enum PdfSource {
        LOCAL,
        MEUDANFE
    }

    private enum FiscalDocumentModel {
        NFE("55"),
        NFCE("65"),
        UNKNOWN("");

        private final String codigo;

        FiscalDocumentModel(String codigo) {
            this.codigo = codigo;
        }

        private static FiscalDocumentModel fromCodigo(String codigo) {
            String normalized = codigo != null ? codigo.trim() : "";
            for (FiscalDocumentModel value : values()) {
                if (value.codigo.equals(normalized)) {
                    return value;
                }
            }
            return UNKNOWN;
        }
    }

    private record GeneratedPdf(byte[] pdfBytes, PdfSource source) {
    }

    private record StoredDocumentResult(NfceStoredDocument document, PdfSource source) {
    }

}
