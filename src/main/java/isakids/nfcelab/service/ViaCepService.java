package isakids.nfcelab.service;

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

import isakids.nfcelab.config.ViaCepProperties;
import isakids.nfcelab.dto.EnderecoCepLookupResponse;
import isakids.nfcelab.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ViaCepService {

    private final ViaCepProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ViaCepService(ViaCepProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    ViaCepService(ViaCepProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public EnderecoCepLookupResponse consultarPorCep(String cep) {
        String normalizedCep = normalizeCep(cep);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + "/" + normalizedCep + "/json/"))
                    .timeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 5)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("ViaCEP retornou HTTP " + response.statusCode() + ": " + truncate(response.body(), 500));
            }

            ViaCepPayload payload = objectMapper.readValue(response.body(), ViaCepPayload.class);
            if (payload == null || Boolean.TRUE.equals(payload.erro())) {
                throw new ResourceNotFoundException("CEP nao encontrado no ViaCEP.");
            }

            return new EnderecoCepLookupResponse(
                    payload.cep(),
                    payload.logradouro(),
                    buildLogradouroCompleto(payload.logradouro()),
                    emptyToNull(payload.complemento()),
                    payload.bairro(),
                    payload.localidade(),
                    payload.uf(),
                    payload.estado(),
                    payload.regiao(),
                    payload.ibge(),
                    payload.gia(),
                    payload.ddd(),
                    payload.siafi());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha consultando CEP no ViaCEP.", e);
        }
    }

    private String normalizeCep(String cep) {
        String normalized = cep == null ? null : cep.replaceAll("\\D", "");
        if (!StringUtils.hasText(normalized) || normalized.length() != 8) {
            throw new IllegalArgumentException("cep deve ter 8 digitos.");
        }
        return normalized;
    }

    private String buildLogradouroCompleto(String logradouro) {
        return emptyToNull(logradouro);
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://viacep.com.br/ws";
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViaCepPayload(
            String cep,
            String logradouro,
            String complemento,
            String bairro,
            String localidade,
            String uf,
            String estado,
            String regiao,
            String ibge,
            String gia,
            String ddd,
            String siafi,
            Boolean erro) {
    }
}
