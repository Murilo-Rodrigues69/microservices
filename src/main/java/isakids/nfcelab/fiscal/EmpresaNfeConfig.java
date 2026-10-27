package isakids.nfcelab.fiscal;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.DFConfig;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFeConfig;

import isakids.nfcelab.entity.EmpresaEntity;

public class EmpresaNfeConfig extends NFeConfig {

    private final EmpresaEntity empresa;
    private KeyStore certificadoKeyStore;
    private KeyStore cadeiaKeyStore;

    public EmpresaNfeConfig(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    @Override
    public DFAmbiente getAmbiente() {
        return DFAmbiente.valueOf(empresa.getAmbiente().name());
    }

    @Override
    public DFModelo getModelo() {
        return DFModelo.valueOf(empresa.getModelo().name());
    }

    @Override
    public DFUnidadeFederativa getCUF() {
        return DFUnidadeFederativa.valueOf(requireText(empresa.getUf(), "empresa.uf").toUpperCase(Locale.ROOT));
    }

    @Override
    public String getCertificadoSenha() {
        return resolveSecretText(empresa.getCertificadoSenha(), "empresa.certificadoSenha");
    }

    @Override
    public String getCadeiaCertificadosSenha() {
        return resolveSecretText(empresa.getCadeiaSenha(), "empresa.cadeiaSenha");
    }

    @Override
    public String getCodigoSegurancaContribuinte() {
        return isNfce() && StringUtils.hasText(empresa.getCsc()) ? resolveSecretText(empresa.getCsc(), "empresa.csc") : null;
    }

    @Override
    public Integer getCodigoSegurancaContribuinteID() {
        return isNfce() && empresa.getCscId() != null && empresa.getCscId() > 0 ? empresa.getCscId() : null;
    }

    @Override
    public KeyStore getCertificadoKeyStore() throws KeyStoreException {
        if (certificadoKeyStore == null) {
            certificadoKeyStore = carregarKeyStorePKCS12(empresa.getCertificadoPfxPath(),
                    "empresa.certificadoPfxPath", getCertificadoSenha());
        }
        return certificadoKeyStore;
    }

    @Override
    public KeyStore getCadeiaCertificadosKeyStore() throws KeyStoreException {
        if (cadeiaKeyStore == null) {
            cadeiaKeyStore = carregarKeyStoreJKS(empresa.getCadeiaJksPath(),
                    "empresa.cadeiaJksPath", getCadeiaCertificadosSenha());
        }
        return cadeiaKeyStore;
    }

    @Override
    public TimeZone getTimeZone() {
        return DFConfig.TIMEZONE_SP;
    }

    private boolean isNfce() {
        return DFModelo.NFCE.equals(getModelo());
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Configure " + fieldName + " antes de utilizar a empresa " + empresa.getNome() + ".");
        }
        String normalized = value.replace("\uFEFF", "").trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalStateException("Configure " + fieldName + " antes de utilizar a empresa " + empresa.getNome() + ".");
        }
        return normalized;
    }

    private KeyStore carregarKeyStorePKCS12(String source, String fieldName, String senha) throws KeyStoreException {
        try (InputStream is = openKeyStoreInputStream(source, fieldName)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, senha.toCharArray());
            return ks;
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new KeyStoreException("Falha ao carregar certificado PKCS12 (.pfx) em " + fieldName + ".", e);
        }
    }

    private KeyStore carregarKeyStoreJKS(String source, String fieldName, String senha) throws KeyStoreException {
        try (InputStream is = openKeyStoreInputStream(source, fieldName)) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(is, senha.toCharArray());
            return ks;
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new KeyStoreException("Falha ao carregar cadeia JKS em " + fieldName + ".", e);
        }
    }

    private InputStream openKeyStoreInputStream(String rawSource, String fieldName) throws IOException, KeyStoreException {
        String source = requireText(rawSource, fieldName);
        if (source.startsWith("env://")) {
            String envName = source.substring("env://".length()).trim();
            if (!StringUtils.hasText(envName)) {
                throw new KeyStoreException("Nome da variavel nao informado em " + fieldName + ".");
            }
            String envValue = System.getenv(envName);
            if (!StringUtils.hasText(envValue)) {
                envValue = System.getProperty(envName);
            }
            if (!StringUtils.hasText(envValue)) {
                throw new KeyStoreException("Variavel " + envName + " nao encontrada para " + fieldName + ".");
            }
            return openKeyStoreInputStream(normalizeInlineKeyStoreSource(envValue), fieldName);
        }
        if (source.startsWith("base64:")) {
            String base64 = source.substring("base64:".length()).trim();
            if (!StringUtils.hasText(base64)) {
                throw new KeyStoreException("Conteudo base64 vazio em " + fieldName + ".");
            }
            return new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        }
        if (source.startsWith("classpath:")) {
            String resourcePath = source.substring("classpath:".length()).trim();
            if (!StringUtils.hasText(resourcePath)) {
                throw new KeyStoreException("Recurso classpath nao informado em " + fieldName + ".");
            }
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                throw new KeyStoreException("Recurso classpath nao encontrado em " + fieldName + ": " + resourcePath);
            }
            return resource.getInputStream();
        }

        if (source.startsWith("file://")) {
            return new FileInputStream(resolveFilePath(source).toFile());
        }

        if (looksLikeBase64(source)) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(source));
        }

        String normalized = source.replace('\\', '/');
        Path path = Paths.get(normalized);
        if (!Files.exists(path)) {
            throw new KeyStoreException("Arquivo nao encontrado em " + fieldName + ": " + path);
        }
        return new FileInputStream(path.toFile());
    }

    private String resolveSecretText(String rawSource, String fieldName) {
        return resolveSecretText(rawSource, fieldName, 0);
    }

    private String resolveSecretText(String rawSource, String fieldName, int depth) {
        if (depth > 3) {
            throw new IllegalStateException("Referencia de segredo muito profunda em " + fieldName + ".");
        }

        String source = requireText(rawSource, fieldName);
        if (source.startsWith("env://")) {
            String envName = source.substring("env://".length()).trim();
            if (!StringUtils.hasText(envName)) {
                throw new IllegalStateException("Nome da variavel nao informado em " + fieldName + ".");
            }
            String envValue = System.getenv(envName);
            if (!StringUtils.hasText(envValue)) {
                envValue = System.getProperty(envName);
            }
            if (!StringUtils.hasText(envValue)) {
                throw new IllegalStateException("Variavel " + envName + " nao encontrada para " + fieldName + ".");
            }
            return resolveSecretText(envValue, fieldName, depth + 1);
        }

        if (source.startsWith("file://")) {
            try {
                String fileContent = Files.readString(resolveFilePath(source), StandardCharsets.UTF_8);
                return resolveSecretText(fileContent, fieldName, depth + 1);
            } catch (IOException e) {
                throw new IllegalStateException("Falha ao carregar segredo em arquivo para " + fieldName + ".", e);
            }
        }

        return source;
    }

    private Path resolveFilePath(String source) throws IOException {
        String pathText = source.substring("file://".length()).trim();
        if (!StringUtils.hasText(pathText)) {
            throw new IOException("Caminho de arquivo nao informado.");
        }
        Path path = Paths.get(pathText.replace('\\', '/')).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IOException("Arquivo nao encontrado: " + path);
        }
        return path;
    }

    private String normalizeInlineKeyStoreSource(String rawValue) {
        String normalized = requireText(rawValue, "keystore.inline");
        if (normalized.startsWith("base64:")
                || normalized.startsWith("classpath:")
                || normalized.startsWith("env://")) {
            return normalized;
        }
        return looksLikeBase64(normalized) ? "base64:" + normalized : normalized;
    }

    private boolean looksLikeBase64(String value) {
        String normalized = requireText(value, "keystore.base64");
        if ((normalized.length() % 4) != 0) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean isBase64Char = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '+'
                    || c == '/'
                    || c == '=';
            if (!isBase64Char) {
                return false;
            }
        }
        return true;
    }
}
