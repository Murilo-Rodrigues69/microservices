package isakids.nfcelab.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import isakids.nfcelab.dto.EmpresaRequest;
import isakids.nfcelab.dto.EmpresaResponse;
import isakids.nfcelab.entity.EmpresaEntity;
import isakids.nfcelab.entity.FiscalModelo;
import isakids.nfcelab.exception.ResourceNotFoundException;
import isakids.nfcelab.repository.EmpresaRepository;

@Service
@Transactional(readOnly = true)
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public EmpresaResponse buscar(Long empresaId) {
        return toResponse(requireEmpresa(empresaId));
    }

    @Transactional
    public EmpresaResponse criar(EmpresaRequest request) {
        EmpresaEntity empresa = new EmpresaEntity();
        applyRequest(empresa, request, false);
        validarCnpjUnico(empresa);
        return toResponse(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaResponse atualizar(Long empresaId, EmpresaRequest request) {
        EmpresaEntity empresa = requireEmpresa(empresaId);
        applyRequest(empresa, request, true);
        validarCnpjUnico(empresa);
        return toResponse(empresaRepository.save(empresa));
    }

    public EmpresaEntity requireEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada para o id " + empresaId + "."));
    }

    public EmpresaEntity resolveEmpresaAtiva(Long empresaId) {
        if (empresaId != null) {
            return empresaRepository.findByIdAndAtivoTrue(empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa ativa nao encontrada para o id " + empresaId + "."));
        }

        List<EmpresaEntity> ativas = empresaRepository.findByAtivoTrueOrderByNomeAsc();
        if (ativas.isEmpty()) {
            throw new IllegalStateException("Nenhuma empresa ativa cadastrada para operar.");
        }
        if (ativas.size() > 1) {
            throw new IllegalArgumentException("Informe empresaId para a operacao. Existem multiplas empresas ativas.");
        }
        return ativas.get(0);
    }

    private void validarCnpjUnico(EmpresaEntity empresa) {
        empresaRepository.findByCnpj(empresa.getCnpj())
                .filter(existing -> !existing.getId().equals(empresa.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe uma empresa cadastrada com o CNPJ informado.");
                });
    }

    private void applyRequest(EmpresaEntity empresa, EmpresaRequest request, boolean preserveSecretsOnBlank) {
        empresa.setNome(requireText(request.getNome(), "nome"));
        empresa.setCnpj(normalizeDigits(request.getCnpj(), 14, "cnpj"));
        empresa.setRazaoSocial(requireText(request.getRazaoSocial(), "razaoSocial"));
        empresa.setNomeFantasia(trimToNull(request.getNomeFantasia()));
        empresa.setInscricaoEstadual(requireText(request.getInscricaoEstadual(), "inscricaoEstadual"));
        empresa.setSerie(requireText(request.getSerie(), "serie"));
        empresa.setNaturezaOperacao(requireText(request.getNaturezaOperacao(), "naturezaOperacao"));
        empresa.setCep(normalizeDigits(request.getCep(), 8, "cep"));
        empresa.setLogradouro(requireText(request.getLogradouro(), "logradouro"));
        empresa.setNumero(requireText(request.getNumero(), "numero"));
        empresa.setBairro(requireText(request.getBairro(), "bairro"));
        empresa.setCodigoMunicipio(normalizeDigits(request.getCodigoMunicipio(), null, "codigoMunicipio"));
        empresa.setDescricaoMunicipio(requireText(request.getDescricaoMunicipio(), "descricaoMunicipio"));
        empresa.setUf(normalizeUf(request.getUf()));
        empresa.setModelo(request.getModelo());
        empresa.setAmbiente(request.getAmbiente());
        empresa.setCertificadoPfxPath(requireText(request.getCertificadoPfxPath(), "certificadoPfxPath"));
        empresa.setCadeiaJksPath(requireText(request.getCadeiaJksPath(), "cadeiaJksPath"));
        empresa.setAtivo(request.getAtivo() == null || request.getAtivo());

        if (!preserveSecretsOnBlank || StringUtils.hasText(request.getCertificadoSenha())) {
            empresa.setCertificadoSenha(requireText(request.getCertificadoSenha(), "certificadoSenha"));
        } else if (!StringUtils.hasText(empresa.getCertificadoSenha())) {
            throw new IllegalArgumentException("Informe certificadoSenha para a empresa.");
        }

        if (!preserveSecretsOnBlank || StringUtils.hasText(request.getCadeiaSenha())) {
            empresa.setCadeiaSenha(requireText(request.getCadeiaSenha(), "cadeiaSenha"));
        } else if (!StringUtils.hasText(empresa.getCadeiaSenha())) {
            throw new IllegalArgumentException("Informe cadeiaSenha para a empresa.");
        }

        if (FiscalModelo.NFCE.equals(request.getModelo())) {
            if (!preserveSecretsOnBlank || StringUtils.hasText(request.getCsc())) {
                empresa.setCsc(requireText(request.getCsc(), "csc"));
            } else if (!StringUtils.hasText(empresa.getCsc())) {
                throw new IllegalArgumentException("Informe csc para a empresa NFC-e.");
            }

            Integer cscId = request.getCscId() != null ? request.getCscId() : empresa.getCscId();
            if (cscId == null || cscId <= 0) {
                throw new IllegalArgumentException("Informe cscId maior que zero para a empresa NFC-e.");
            }
            empresa.setCscId(cscId);
        } else {
            empresa.setCsc(null);
            empresa.setCscId(0);
        }
    }

    private EmpresaResponse toResponse(EmpresaEntity empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getCnpj(),
                empresa.getRazaoSocial(),
                empresa.getNomeFantasia(),
                empresa.getInscricaoEstadual(),
                empresa.getSerie(),
                empresa.getNaturezaOperacao(),
                empresa.getCep(),
                empresa.getLogradouro(),
                empresa.getNumero(),
                empresa.getBairro(),
                empresa.getCodigoMunicipio(),
                empresa.getDescricaoMunicipio(),
                empresa.getUf(),
                empresa.getModelo(),
                empresa.getAmbiente(),
                empresa.getCertificadoPfxPath(),
                empresa.getCadeiaJksPath(),
                empresa.getCscId(),
                empresa.isAtivo(),
                StringUtils.hasText(empresa.getCertificadoSenha()),
                StringUtils.hasText(empresa.getCadeiaSenha()),
                StringUtils.hasText(empresa.getCsc()),
                empresa.getCreatedAt(),
                empresa.getUpdatedAt());
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " deve ser informado.");
        }
        return value.trim();
    }

    private String normalizeUf(String value) {
        String normalized = requireText(value, "uf").toUpperCase();
        if (normalized.length() != 2) {
            throw new IllegalArgumentException("uf deve ter 2 caracteres.");
        }
        return normalized;
    }

    private String normalizeDigits(String value, Integer expectedSize, String fieldName) {
        String normalized = requireText(value, fieldName).replaceAll("\\D", "");
        if (expectedSize != null && normalized.length() != expectedSize) {
            throw new IllegalArgumentException(fieldName + " deve ter " + expectedSize + " digitos.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
