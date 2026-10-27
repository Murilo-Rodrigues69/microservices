package isakids.nfcelab.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalAmbiente;
import isakids.nfcelab.entity.FiscalModelo;

class EmpresaNfeConfigTests {

    @TempDir
    Path tempDir;

    @Test
    void carregaCertificadosViaVariavelDeAmbienteComConteudoBase64() throws Exception {
        String pfxProperty = "TEST_NFE_CERTIFICADO_PFX_BASE64";
        String jksProperty = "TEST_NFE_CADEIA_JKS_BASE64";
        System.setProperty(pfxProperty, "base64:" + base64KeyStore("PKCS12", "123456"));
        System.setProperty(jksProperty, "base64:" + base64KeyStore("JKS", "senha"));
        try {
            EmpresaEntity empresa = new EmpresaEntity();
            empresa.setNome("EMPRESA TESTE");
            empresa.setUf("GO");
            empresa.setModelo(FiscalModelo.NFCE);
            empresa.setAmbiente(FiscalAmbiente.HOMOLOGACAO);
            empresa.setCertificadoPfxPath("env://" + pfxProperty);
            empresa.setCertificadoSenha("123456");
            empresa.setCadeiaJksPath("env://" + jksProperty);
            empresa.setCadeiaSenha("senha");

            EmpresaNfeConfig config = new EmpresaNfeConfig(empresa);

            assertThat(config.getCertificadoKeyStore()).isNotNull();
            assertThat(config.getCadeiaCertificadosKeyStore()).isNotNull();
        } finally {
            System.clearProperty(pfxProperty);
            System.clearProperty(jksProperty);
        }
    }

    @Test
    void carregaCertificadosViaVariavelDeAmbienteComBase64Puro() throws Exception {
        String pfxProperty = "TEST_NFE_CERTIFICADO_PFX_BASE64_RAW";
        String jksProperty = "TEST_NFE_CADEIA_JKS_BASE64_RAW";
        System.setProperty(pfxProperty, base64KeyStore("PKCS12", "123456"));
        System.setProperty(jksProperty, base64KeyStore("JKS", "senha"));
        try {
            EmpresaEntity empresa = new EmpresaEntity();
            empresa.setNome("EMPRESA TESTE");
            empresa.setUf("GO");
            empresa.setModelo(FiscalModelo.NFCE);
            empresa.setAmbiente(FiscalAmbiente.HOMOLOGACAO);
            empresa.setCertificadoPfxPath("env://" + pfxProperty);
            empresa.setCertificadoSenha("123456");
            empresa.setCadeiaJksPath("env://" + jksProperty);
            empresa.setCadeiaSenha("senha");

            EmpresaNfeConfig config = new EmpresaNfeConfig(empresa);

            assertThat(config.getCertificadoKeyStore()).isNotNull();
            assertThat(config.getCadeiaCertificadosKeyStore()).isNotNull();
        } finally {
            System.clearProperty(pfxProperty);
            System.clearProperty(jksProperty);
        }
    }

    @Test
    void carregaSenhasECscViaArquivoProtegido() throws Exception {
        String pfxProperty = "TEST_NFE_CERTIFICADO_PFX_BASE64_WITH_FILE_SECRET";
        String jksProperty = "TEST_NFE_CADEIA_JKS_BASE64_WITH_FILE_SECRET";
        System.setProperty(pfxProperty, base64KeyStore("PKCS12", "123456"));
        System.setProperty(jksProperty, base64KeyStore("JKS", "senha"));
        Path certificadoSenha = tempDir.resolve("certificado-senha");
        Path cadeiaSenha = tempDir.resolve("cadeia-senha");
        Path csc = tempDir.resolve("csc");
        Files.writeString(certificadoSenha, "123456\n", StandardCharsets.UTF_8);
        Files.writeString(cadeiaSenha, "senha\n", StandardCharsets.UTF_8);
        Files.writeString(csc, "TOKEN-CSC\n", StandardCharsets.UTF_8);

        try {
            EmpresaEntity empresa = new EmpresaEntity();
            empresa.setNome("EMPRESA TESTE");
            empresa.setUf("GO");
            empresa.setModelo(FiscalModelo.NFCE);
            empresa.setAmbiente(FiscalAmbiente.HOMOLOGACAO);
            empresa.setCertificadoPfxPath("env://" + pfxProperty);
            empresa.setCertificadoSenha(fileReference(certificadoSenha));
            empresa.setCadeiaJksPath("env://" + jksProperty);
            empresa.setCadeiaSenha(fileReference(cadeiaSenha));
            empresa.setCsc(fileReference(csc));
            empresa.setCscId(1);

            EmpresaNfeConfig config = new EmpresaNfeConfig(empresa);

            assertThat(config.getCertificadoKeyStore()).isNotNull();
            assertThat(config.getCadeiaCertificadosKeyStore()).isNotNull();
            assertThat(config.getCodigoSegurancaContribuinte()).isEqualTo("TOKEN-CSC");
        } finally {
            System.clearProperty(pfxProperty);
            System.clearProperty(jksProperty);
        }
    }

    private static String base64KeyStore(String type, String senha) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(null, senha.toCharArray());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        keyStore.store(outputStream, senha.toCharArray());
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private String fileReference(Path path) {
        return "file://" + path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
