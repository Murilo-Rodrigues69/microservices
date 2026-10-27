package isakids.nfcelab.model;

public record ProductData(
        Long id,
        String descricao,
        String codigoBarras,
        String ncm,
        String cest,
        String origem,
        String cfop,
        String unidadeComercial) {
}
