package projetopdv.posvenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class DevolucaoItem {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final int idDevolucao;
    private final int idVenda;
    private final int idItemVenda;
    private final int quantidadeDevolvida;
    private final BigDecimal valorUnitario;
    private final BigDecimal valorTotal;
    private final String motivo;
    private final LocalDateTime dataHoraDevolucao;
    private final Integer idValeCompra;

    public DevolucaoItem(int idDevolucao, int idVenda, int idItemVenda, int quantidadeDevolvida,
                         BigDecimal valorUnitario, BigDecimal valorTotal, String motivo,
                         LocalDateTime dataHoraDevolucao, Integer idValeCompra) {
        if (idVenda <= 0) {
            throw new IllegalArgumentException("ID da venda inválido.");
        }
        if (idItemVenda <= 0) {
            throw new IllegalArgumentException("ID do item da venda inválido.");
        }
        if (quantidadeDevolvida <= 0) {
            throw new IllegalArgumentException("A quantidade devolvida deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor unitário reembolsado não pode ser nulo ou negativo.");
        }
        if (dataHoraDevolucao == null) {
            throw new IllegalArgumentException("Data/hora da devolução não pode ser nula.");
        }

        this.idDevolucao = idDevolucao;
        this.idVenda = idVenda;
        this.idItemVenda = idItemVenda;
        this.quantidadeDevolvida = quantidadeDevolvida;
        this.valorUnitario = valorUnitario.setScale(2, RoundingMode.HALF_UP);
        this.valorTotal = (valorTotal != null)
                ? valorTotal.setScale(2, RoundingMode.HALF_UP)
                : this.valorUnitario.multiply(BigDecimal.valueOf(quantidadeDevolvida)).setScale(2, RoundingMode.HALF_UP);
        this.motivo = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Não especificado";
        this.dataHoraDevolucao = dataHoraDevolucao;
        this.idValeCompra = (idValeCompra != null && idValeCompra > 0) ? idValeCompra : null;
    }

    public int getIdDevolucao() {
        return idDevolucao;
    }

    public int getIdVenda() {
        return idVenda;
    }

    public int getIdItemVenda() {
        return idItemVenda;
    }

    public int getQuantidadeDevolvida() {
        return quantidadeDevolvida;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getDataHoraDevolucao() {
        return dataHoraDevolucao;
    }

    public Integer getIdValeCompra() {
        return idValeCompra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DevolucaoItem that = (DevolucaoItem) o;
        return idDevolucao == that.idDevolucao && idDevolucao > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDevolucao);
    }

    @Override
    public String toString() {
        return String.format("Devolução #%d | Venda #%d | Item #%d | Qtd: %d | Total: R$ %.2f | Motivo: %s",
                idDevolucao, idVenda, idItemVenda, quantidadeDevolvida, valorTotal, motivo);
    }
}
