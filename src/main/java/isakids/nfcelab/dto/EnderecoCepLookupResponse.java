package isakids.nfcelab.dto;

public record EnderecoCepLookupResponse(
        String cep,
        String logradouro,
        String logradouroCompleto,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        String estado,
        String regiao,
        String ibge,
        String gia,
        String ddd,
        String siafi) {
}
