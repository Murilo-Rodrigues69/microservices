package isakids.nfcelab.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public record SaleData(
        Long vendaId,
        String numeroNota,
        LocalDateTime dataHora,
        BigDecimal desconto,
        FormaPagamento formaPagamento,
        String observacao,
        List<SaleItemData> itens) {

    public BigDecimal descontoOrZero() {
        return desconto != null ? desconto : BigDecimal.ZERO;
    }

    public BigDecimal totalProdutos() {
        if (itens == null || itens.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return itens.stream()
                .map(SaleItemData::subtotalOrCalculated)
                .map(this::normalizeMoney)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    public List<BigDecimal> descontosPorItem() {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }

        List<BigDecimal> descontosInformados = itens.stream()
                .map(SaleItemData::descontoOrZero)
                .map(this::normalizeMoney)
                .toList();

        BigDecimal somaDescontosInformados = descontosInformados.stream()
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        if (somaDescontosInformados.signum() > 0) {
            validarDescontosPorItem(descontosInformados);
            return descontosInformados;
        }

        BigDecimal descontoTotal = normalizeMoney(descontoOrZero());
        if (descontoTotal.signum() <= 0) {
            return itens.stream()
                    .map(item -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .toList();
        }

        return ratearDesconto(descontoTotal);
    }

    public BigDecimal descontoTotalAplicado() {
        return descontosPorItem().stream()
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    public BigDecimal valorTotalNota() {
        BigDecimal total = totalProdutos().subtract(descontoTotalAplicado());
        if (total.signum() < 0) {
            throw new IllegalArgumentException("Desconto total nao pode ser maior que o total dos itens.");
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarDescontosPorItem(List<BigDecimal> descontosPorItem) {
        BigDecimal totalDesconto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (int i = 0; i < itens.size(); i++) {
            BigDecimal subtotalItem = normalizeMoney(itens.get(i).subtotalOrCalculated());
            BigDecimal descontoItem = descontosPorItem.get(i);
            if (descontoItem.compareTo(subtotalItem) > 0) {
                throw new IllegalArgumentException(
                        "Desconto do item " + (i + 1) + " nao pode ser maior que o subtotal do item.");
            }
            totalDesconto = totalDesconto.add(descontoItem);
        }

        if (totalDesconto.compareTo(totalProdutos()) > 0) {
            throw new IllegalArgumentException("Desconto total nao pode ser maior que o total dos itens.");
        }
    }

    private List<BigDecimal> ratearDesconto(BigDecimal descontoTotal) {
        BigDecimal totalProdutos = totalProdutos();
        if (descontoTotal.compareTo(totalProdutos) > 0) {
            throw new IllegalArgumentException("Desconto total nao pode ser maior que o total dos itens.");
        }

        List<Long> subtotaisEmCentavos = itens.stream()
                .map(SaleItemData::subtotalOrCalculated)
                .map(this::normalizeMoney)
                .map(this::toCents)
                .toList();

        long totalBrutoEmCentavos = subtotaisEmCentavos.stream()
                .mapToLong(Long::longValue)
                .sum();
        long descontoEmCentavos = toCents(descontoTotal);

        if (totalBrutoEmCentavos <= 0 || descontoEmCentavos <= 0) {
            return itens.stream()
                    .map(item -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .toList();
        }

        long[] descontosBase = new long[itens.size()];
        long[] restos = new long[itens.size()];
        long alocado = 0;

        for (int i = 0; i < subtotaisEmCentavos.size(); i++) {
            long subtotalEmCentavos = subtotaisEmCentavos.get(i);
            long numerador = descontoEmCentavos * subtotalEmCentavos;
            descontosBase[i] = numerador / totalBrutoEmCentavos;
            restos[i] = numerador % totalBrutoEmCentavos;
            alocado += descontosBase[i];
        }

        long restante = descontoEmCentavos - alocado;
        while (restante > 0) {
            int melhorIndice = 0;
            for (int i = 1; i < restos.length; i++) {
                if (restos[i] > restos[melhorIndice]) {
                    melhorIndice = i;
                }
            }
            descontosBase[melhorIndice]++;
            restos[melhorIndice] = Long.MIN_VALUE;
            restante--;
        }

        return java.util.Arrays.stream(descontosBase)
                .mapToObj(this::fromCents)
                .toList();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private long toCents(BigDecimal value) {
        return normalizeMoney(value)
                .movePointRight(2)
                .longValueExact();
    }

    private BigDecimal fromCents(long value) {
        return BigDecimal.valueOf(value, 2).setScale(2, RoundingMode.HALF_UP);
    }
}
