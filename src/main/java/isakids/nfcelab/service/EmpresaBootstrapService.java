package isakids.nfcelab.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import isakids.nfcelab.config.LegacyFiscalBootstrapProperties;
import isakids.nfcelab.config.NfceIssuerProperties;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.repository.EmpresaRepository;

@Component
public class EmpresaBootstrapService implements ApplicationRunner {

    private final EmpresaRepository empresaRepository;
    private final LegacyFiscalBootstrapProperties legacyFiscalProperties;
    private final NfceIssuerProperties issuerProperties;

    public EmpresaBootstrapService(EmpresaRepository empresaRepository, LegacyFiscalBootstrapProperties legacyFiscalProperties,
            NfceIssuerProperties issuerProperties) {
        this.empresaRepository = empresaRepository;
        this.legacyFiscalProperties = legacyFiscalProperties;
        this.issuerProperties = issuerProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (empresaRepository.count() > 0 || !StringUtils.hasText(issuerProperties.getCnpj())) {
            return;
        }

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setNome(firstNonBlank(issuerProperties.getNomeFantasia(), issuerProperties.getRazaoSocial(), "EMPRESA"));
        empresa.setCnpj(onlyDigits(issuerProperties.getCnpj()));
        empresa.setRazaoSocial(firstNonBlank(issuerProperties.getRazaoSocial(), empresa.getNome()));
        empresa.setNomeFantasia(firstNonBlank(issuerProperties.getNomeFantasia(), empresa.getNome()));
        empresa.setInscricaoEstadual(firstNonBlank(issuerProperties.getInscricaoEstadual(), "ISENTO"));
        empresa.setSerie(firstNonBlank(issuerProperties.getSerie(), "1"));
        empresa.setNaturezaOperacao(firstNonBlank(issuerProperties.getNaturezaOperacao(), "VENDA DE MERCADORIA"));
        empresa.setCep(onlyDigits(issuerProperties.getCep()));
        empresa.setLogradouro(firstNonBlank(issuerProperties.getLogradouro(), "NAO INFORMADO"));
        empresa.setNumero(firstNonBlank(issuerProperties.getNumero(), "S/N"));
        empresa.setBairro(firstNonBlank(issuerProperties.getBairro(), "NAO INFORMADO"));
        empresa.setCodigoMunicipio(firstNonBlank(issuerProperties.getCodigoMunicipio(), "0000000"));
        empresa.setDescricaoMunicipio(firstNonBlank(issuerProperties.getDescricaoMunicipio(), "NAO INFORMADO"));
        empresa.setUf(firstNonBlank(legacyFiscalProperties.getUf(), "GO"));
        empresa.setModelo(legacyFiscalProperties.getModelo());
        empresa.setAmbiente(legacyFiscalProperties.getAmbiente());
        empresa.setCertificadoPfxPath(legacyFiscalProperties.getCertificado().getPfxPath());
        empresa.setCertificadoSenha(firstNonBlank(legacyFiscalProperties.getCertificado().getSenha(), ""));
        empresa.setCadeiaJksPath(legacyFiscalProperties.getCadeia().getJksPath());
        empresa.setCadeiaSenha(firstNonBlank(legacyFiscalProperties.getCadeia().getSenha(), ""));
        empresa.setCsc(trimToNull(legacyFiscalProperties.getCsc()));
        empresa.setCscId(legacyFiscalProperties.getCscId());
        empresa.setAtivo(true);

        empresaRepository.save(empresa);
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

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
