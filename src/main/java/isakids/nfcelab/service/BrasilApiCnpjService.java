package isakids.nfcelab.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import isakids.nfcelab.config.BrasilApiProperties;
import isakids.nfcelab.dto.EmpresaCnpjLookupResponse;
import isakids.nfcelab.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

@Service
public class BrasilApiCnpjService {

    private final BrasilApiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public BrasilApiCnpjService(BrasilApiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    BrasilApiCnpjService(BrasilApiProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public EmpresaCnpjLookupResponse consultarPorCnpj(String cnpj) {
        String normalizedCnpj = normalizeCnpj(cnpj);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + "/cnpj/v1/" + normalizedCnpj))
                    .timeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 5)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404) {
                throw new ResourceNotFoundException("CNPJ nao encontrado na BrasilAPI.");
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("BrasilAPI retornou HTTP " + response.statusCode() + ": " + truncate(response.body(), 500));
            }

            BrasilApiCnpjPayload payload = objectMapper.readValue(response.body(), BrasilApiCnpjPayload.class);
            return mapToResponse(payload);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha consultando CNPJ na BrasilAPI.", e);
        }
    }

    private EmpresaCnpjLookupResponse mapToResponse(BrasilApiCnpjPayload payload) {
        if (payload == null) {
            throw new IllegalStateException("BrasilAPI nao retornou dados para o CNPJ informado.");
        }

        return new EmpresaCnpjLookupResponse(
                payload.cnpj(),
                payload.razaoSocial(),
                payload.nomeFantasia(),
                payload.situacaoCadastral() != null ? String.valueOf(payload.situacaoCadastral()) : null,
                payload.descricaoSituacaoCadastral(),
                payload.dataInicioAtividade(),
                payload.naturezaJuridica(),
                payload.porte(),
                payload.capitalSocial(),
                payload.email(),
                payload.telefone1(),
                payload.telefone2(),
                payload.cep(),
                payload.tipoLogradouro(),
                payload.logradouro(),
                buildLogradouroCompleto(payload.tipoLogradouro(), payload.logradouro()),
                payload.numero(),
                emptyToNull(payload.complemento()),
                payload.bairro(),
                payload.municipio(),
                payload.codigoMunicipioIbge() != null ? String.valueOf(payload.codigoMunicipioIbge()) : null,
                payload.uf());
    }

    private String normalizeCnpj(String cnpj) {
        String normalized = cnpj == null ? null : cnpj.replaceAll("\\D", "");
        if (!StringUtils.hasText(normalized) || normalized.length() != 14) {
            throw new IllegalArgumentException("cnpj deve ter 14 digitos.");
        }
        return normalized;
    }

    private String buildLogradouroCompleto(String tipoLogradouro, String logradouro) {
        if (!StringUtils.hasText(tipoLogradouro)) {
            return emptyToNull(logradouro);
        }
        if (!StringUtils.hasText(logradouro)) {
            return tipoLogradouro.trim();
        }
        return tipoLogradouro.trim() + " " + logradouro.trim();
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://brasilapi.com.br/api";
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BrasilApiCnpjPayload(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            @JsonProperty("situacao_cadastral") Integer situacaoCadastral,
            @JsonProperty("descricao_situacao_cadastral") String descricaoSituacaoCadastral,
            @JsonProperty("data_inicio_atividade") String dataInicioAtividade,
            @JsonProperty("natureza_juridica") String naturezaJuridica,
            String porte,
            @JsonProperty("capital_social") BigDecimal capitalSocial,
            String email,
            @JsonProperty("ddd_telefone_1") String telefone1,
            @JsonProperty("ddd_telefone_2") String telefone2,
            String cep,
            @JsonProperty("descricao_tipo_de_logradouro") String tipoLogradouro,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String municipio,
            @JsonProperty("codigo_municipio_ibge") Integer codigoMunicipioIbge,
            String uf) {
    }
}
