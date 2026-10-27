package isakids.nfcelab.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.utils.DFCadeiaCertificados;

public final class GerarCadeiasCertificadosHelper {

    private GerarCadeiasCertificadosHelper() {
    }

    public static void main(String[] args) throws Exception {
        String senha = System.getenv("NFE_CADEIA_SENHA");
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException("Configure NFE_CADEIA_SENHA antes de gerar as cadeias.");
        }
        Path outputDir = Paths.get(args.length > 0 ? args[0] : "certificados");
        Files.createDirectories(outputDir);

        Files.write(
                outputDir.resolve("sefaz-producao.jks"),
                DFCadeiaCertificados.geraCadeiaCertificados(DFAmbiente.PRODUCAO, senha));

        Files.write(
                outputDir.resolve("sefaz-homologacao.jks"),
                DFCadeiaCertificados.geraCadeiaCertificados(DFAmbiente.HOMOLOGACAO, senha));
    }
}
