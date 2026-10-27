package isakids.nfcelab.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import isakids.nfcelab.config.NfceDocumentProperties;
import isakids.nfcelab.entity.NotaFiscalDocumentoEntity;
import isakids.nfcelab.repository.NotaFiscalDocumentoRepository;
import isakids.nfcelab.support.SampleAuthorizedXmls;
import tools.jackson.databind.ObjectMapper;

class NfceDocumentServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void salvaXmlEPdfDaNota() throws Exception {
        NfceDocumentProperties properties = new NfceDocumentProperties();
        properties.setOutputDir(tempDir.toString());

        NfceDocumentService service = new NfceDocumentService(
                properties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new MeuDanfeClient(properties, new ObjectMapper()),
                null,
                null,
                null,
                null);
        String chaveAcesso = "52260411222333000144550010000020011000020010";

        NfceDocumentArtifacts artifacts = service.store(chaveAcesso, SampleAuthorizedXmls.authorizedNfe());

        assertThat(Files.exists(artifacts.xmlPath())).isTrue();
        assertThat(Files.exists(artifacts.pdfPath())).isTrue();
        assertThat(service.loadXml(chaveAcesso)).contains("<nfeProc");
        assertThat(service.loadPdf(chaveAcesso)).isNotEmpty();
        assertThat(new String(Files.readAllBytes(artifacts.pdfPath()), 0, 5, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF-");
    }

    @Test
    void substituiPdfLocalPorDanfeDoMeuDanfeQuandoIntegracaoEstaAtiva() throws Exception {
        NfceDocumentProperties localProperties = new NfceDocumentProperties();
        localProperties.setOutputDir(tempDir.toString());
        localProperties.setPdfProvider(NfceDocumentProperties.PdfProvider.LOCAL);

        NfceDocumentService localService = new NfceDocumentService(
                localProperties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new FakeMeuDanfeClient(localProperties, false, null),
                null,
                null,
                null,
                null);
        String chaveAcesso = "52260411222333000144550010000020011000020010";
        localService.store(chaveAcesso, SampleAuthorizedXmls.authorizedNfe());

        Path pdfPath = tempDir.resolve(chaveAcesso + ".pdf");
        byte[] pdfLocal = Files.readAllBytes(pdfPath);

        NfceDocumentProperties meuDanfeProperties = new NfceDocumentProperties();
        meuDanfeProperties.setOutputDir(tempDir.toString());
        meuDanfeProperties.setPdfProvider(NfceDocumentProperties.PdfProvider.AUTO);
        meuDanfeProperties.getMeuDanfe().setApiKey("configured");
        byte[] pdfMeuDanfe = "%PDF-REAL-DANFE".getBytes(StandardCharsets.ISO_8859_1);

        NfceDocumentService meuDanfeService = new NfceDocumentService(
                meuDanfeProperties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new FakeMeuDanfeClient(meuDanfeProperties, true, pdfMeuDanfe),
                null,
                null,
                null,
                null);

        byte[] pdfAtualizado = meuDanfeService.loadPdf(chaveAcesso);

        assertThat(pdfAtualizado).isEqualTo(pdfMeuDanfe);
        assertThat(Files.readAllBytes(pdfPath)).isEqualTo(pdfMeuDanfe);
        assertThat(Files.readString(tempDir.resolve(chaveAcesso + ".pdf.source"), StandardCharsets.UTF_8))
                .isEqualTo("MEUDANFE");
        assertThat(pdfAtualizado).isNotEqualTo(pdfLocal);
    }

    @Test
    void substituiPdfPersistidoNoBancoPorDanfeDoMeuDanfeQuandoIntegracaoEstaAtiva() throws Exception {
        NfceDocumentProperties localProperties = new NfceDocumentProperties();
        localProperties.setOutputDir(tempDir.toString());
        localProperties.setPdfProvider(NfceDocumentProperties.PdfProvider.LOCAL);
        NotaFiscalDocumentoRepository repository = mock(NotaFiscalDocumentoRepository.class);

        final NotaFiscalDocumentoEntity[] persisted = new NotaFiscalDocumentoEntity[1];
        when(repository.findByChaveAcesso("52260411222333000144550010000020011000020010"))
                .thenAnswer(invocation -> Optional.ofNullable(persisted[0]));
        when(repository.save(any(NotaFiscalDocumentoEntity.class))).thenAnswer(invocation -> {
            persisted[0] = invocation.getArgument(0);
            return persisted[0];
        });

        NfceDocumentService localService = new NfceDocumentService(
                localProperties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new FakeMeuDanfeClient(localProperties, false, null),
                repository,
                null,
                null,
                null);
        String chaveAcesso = "52260411222333000144550010000020011000020010";
        localService.store(chaveAcesso, SampleAuthorizedXmls.authorizedNfe());

        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".xml"));
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".pdf"));
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".pdf.source"));

        NfceDocumentProperties meuDanfeProperties = new NfceDocumentProperties();
        meuDanfeProperties.setOutputDir(tempDir.toString());
        meuDanfeProperties.setPdfProvider(NfceDocumentProperties.PdfProvider.AUTO);
        meuDanfeProperties.getMeuDanfe().setApiKey("configured");
        byte[] pdfMeuDanfe = "%PDF-REAL-DANFE".getBytes(StandardCharsets.ISO_8859_1);

        NfceDocumentService meuDanfeService = new NfceDocumentService(
                meuDanfeProperties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new FakeMeuDanfeClient(meuDanfeProperties, true, pdfMeuDanfe),
                repository,
                null,
                null,
                null);

        byte[] pdfAtualizado = meuDanfeService.loadPdf(chaveAcesso);

        assertThat(pdfAtualizado).isEqualTo(pdfMeuDanfe);
        assertThat(Base64.getDecoder().decode(persisted[0].getPdfBase64())).isEqualTo(pdfMeuDanfe);
        assertThat(Files.readAllBytes(tempDir.resolve(chaveAcesso + ".pdf"))).isEqualTo(pdfMeuDanfe);
        assertThat(Files.readString(tempDir.resolve(chaveAcesso + ".pdf.source"), StandardCharsets.UTF_8))
                .isEqualTo("MEUDANFE");
    }

    @Test
    void mantemGeracaoLocalParaNfceMesmoQuandoMeuDanfeEstaConfigurado() throws Exception {
        NfceDocumentProperties properties = new NfceDocumentProperties();
        properties.setOutputDir(tempDir.toString());
        properties.setPdfProvider(NfceDocumentProperties.PdfProvider.AUTO);
        properties.getMeuDanfe().setApiKey("configured");

        byte[] pdfMeuDanfe = "%PDF-REAL-DANFE".getBytes(StandardCharsets.ISO_8859_1);
        FakeMeuDanfeClient meuDanfeClient = new FakeMeuDanfeClient(properties, true, pdfMeuDanfe);
        NfceDocumentService service = new NfceDocumentService(
                properties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                meuDanfeClient,
                null,
                null,
                null,
                null);
        String chaveAcesso = "52260411222333000144650010000020011000020010";

        NfceDocumentArtifacts artifacts = service.store(chaveAcesso, SampleAuthorizedXmls.authorizedNfce());
        byte[] pdfGerado = service.loadPdf(chaveAcesso);

        assertThat(Files.exists(artifacts.pdfPath())).isTrue();
        assertThat(new String(Files.readAllBytes(artifacts.pdfPath()), 0, 5, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF-");
        assertThat(pdfGerado).isNotEqualTo(pdfMeuDanfe);
        assertThat(Files.readString(tempDir.resolve(chaveAcesso + ".pdf.source"), StandardCharsets.UTF_8))
                .isEqualTo("LOCAL");
        assertThat(meuDanfeClient.generateCalls).isZero();
    }

    @Test
    void carregaDocumentosDoBancoQuandoCacheLocalNaoExisteMais() throws Exception {
        NfceDocumentProperties properties = new NfceDocumentProperties();
        properties.setOutputDir(tempDir.toString());
        NotaFiscalDocumentoRepository repository = mock(NotaFiscalDocumentoRepository.class);

        final NotaFiscalDocumentoEntity[] persisted = new NotaFiscalDocumentoEntity[1];
        when(repository.findByChaveAcesso("52260411222333000144650010000020011000020010"))
                .thenAnswer(invocation -> Optional.ofNullable(persisted[0]));
        when(repository.save(any(NotaFiscalDocumentoEntity.class))).thenAnswer(invocation -> {
            persisted[0] = invocation.getArgument(0);
            return persisted[0];
        });

        NfceDocumentService writerService = new NfceDocumentService(
                properties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new MeuDanfeClient(properties, new ObjectMapper()),
                repository,
                null,
                null,
                null);
        String chaveAcesso = "52260411222333000144650010000020011000020010";

        writerService.store(chaveAcesso, SampleAuthorizedXmls.authorizedNfce());
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".xml"));
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".pdf"));
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".pdf.source"));

        NfceDocumentService readerService = new NfceDocumentService(
                properties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new MeuDanfeClient(properties, new ObjectMapper()),
                repository,
                null,
                null,
                null);

        assertThat(readerService.loadXml(chaveAcesso)).contains("<nfeProc");
        assertThat(readerService.loadPdf(chaveAcesso)).isNotEmpty();
        assertThat(Files.exists(tempDir.resolve(chaveAcesso + ".xml"))).isTrue();
        assertThat(Files.exists(tempDir.resolve(chaveAcesso + ".pdf"))).isTrue();
    }

    @Test
    void carregaDocumentosDoR2QuandoBancoECacheLocalNaoExistem() throws Exception {
        NfceDocumentProperties properties = new NfceDocumentProperties();
        properties.setOutputDir(tempDir.toString());
        FakeStorage storage = new FakeStorage();
        String chaveAcesso = "52260411222333000144650010000020011000020010";

        NfceDocumentService writerService = new NfceDocumentService(
                properties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new MeuDanfeClient(properties, new ObjectMapper()),
                null,
                null,
                null,
                storage);
        writerService.store(chaveAcesso, SampleAuthorizedXmls.authorizedNfce());
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".xml"));
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".pdf"));
        Files.deleteIfExists(tempDir.resolve(chaveAcesso + ".pdf.source"));

        NfceDocumentService readerService = new NfceDocumentService(
                properties,
                new NfcePdfRenderer(),
                new NfceDanfeRenderer(),
                new MeuDanfeClient(properties, new ObjectMapper()),
                null,
                null,
                null,
                storage);

        assertThat(readerService.loadXml(chaveAcesso)).contains("<nfeProc");
        assertThat(readerService.loadPdf(chaveAcesso)).isNotEmpty();
        assertThat(Files.exists(tempDir.resolve(chaveAcesso + ".xml"))).isTrue();
        assertThat(Files.exists(tempDir.resolve(chaveAcesso + ".pdf"))).isTrue();
    }

    private static final class FakeMeuDanfeClient extends MeuDanfeClient {

        private final boolean configured;
        private final byte[] pdfBytes;
        private int generateCalls;

        private FakeMeuDanfeClient(NfceDocumentProperties properties, boolean configured, byte[] pdfBytes) {
            super(properties, new ObjectMapper(), HttpClient.newHttpClient());
            this.configured = configured;
            this.pdfBytes = pdfBytes;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public byte[] gerarDanfePdf(String xml) {
            generateCalls++;
            return pdfBytes;
        }
    }

    private static final class FakeStorage implements FiscalDocumentStorage {

        private final Map<String, NfceStoredDocument> documents = new HashMap<>();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void store(String chaveAcesso, String xml, byte[] pdfBytes) {
            documents.put(chaveAcesso, new NfceStoredDocument(xml, pdfBytes));
        }

        @Override
        public Optional<NfceStoredDocument> load(String chaveAcesso) {
            return Optional.ofNullable(documents.get(chaveAcesso));
        }
    }
}
