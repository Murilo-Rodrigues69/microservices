package isakids.nfcelab.model;

public record RecipientData(
        String nome,
        String documento,
        String inscricaoEstadual,
        String cep,
        String logradouro,
        String numero,
        String bairro,
        String codigoMunicipio,
        String descricaoMunicipio,
        String uf,
        String email) {
}
