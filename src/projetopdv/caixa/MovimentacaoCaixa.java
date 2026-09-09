package projetopdv.caixa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class MovimentacaoCaixa {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final int idMovimentacao;
    private final TipoMovimentacaoCaixa tipo;
    private final BigDecimal valor;
    private final LocalDateTime dataHora;
    private final Integer idValeCompra;
    private final Integer idVenda;
    private final String justificativa;
    private final Integer idOperador;

    public MovimentacaoCaixa(int idMovimentacao, TipoMovimentacaoCaixa tipo, BigDecimal valor, LocalDateTime dataHora,
                             Integer idValeCompra, Integer idVenda, String justificativa, Integer idOperador) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de movimentação não pode ser nulo.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da movimentação deve ser maior que zero.");
        }
        if (dataHora == null) {
            throw new IllegalArgumentException("A data/hora da movimentação não pode ser nula.");
        }

        this.idMovimentacao = idMovimentacao;
        this.tipo = tipo;
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.dataHora = dataHora;
        this.idValeCompra = (idValeCompra != null && idValeCompra > 0) ? idValeCompra : null;
        this.idVenda = (idVenda != null && idVenda > 0) ? idVenda : null;
        this.justificativa = justificativa != null ? justificativa.trim() : "";
        this.idOperador = (idOperador != null && idOperador > 0) ? idOperador : null;
    }

    public int getIdMovimentacao() {
        return idMovimentacao;
    }

    public TipoMovimentacaoCaixa getTipo() {
        return tipo;
    }

    public TipoMovimentacaoCaixa getTipoMovimentacao() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Integer getIdValeCompra() {
        return idValeCompra;
    }

    public Integer getIdVenda() {
        return idVenda;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public Integer getIdOperador() {
        return idOperador;
    }

    public boolean isSaida() {
        return tipo == TipoMovimentacaoCaixa.ESTORNO_VENDA_DINHEIRO
                || tipo == TipoMovimentacaoCaixa.SANGRIA_RESGATE_VALE
                || tipo == TipoMovimentacaoCaixa.SANGRIA_OPERACIONAL;
    }

    public boolean isEntrada() {
        return tipo == TipoMovimentacaoCaixa.SUPRIMENTO_TROCO
                || tipo == TipoMovimentacaoCaixa.ABERTURA_CAIXA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovimentacaoCaixa that = (MovimentacaoCaixa) o;
        return idMovimentacao == that.idMovimentacao && idMovimentacao > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMovimentacao);
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s R$ %.2f | %s | %s",
                idMovimentacao,
                dataHora.format(FORMATO_DATA_HORA),
                isSaida() ? "[-] " : "[+] ",
                valor,
                tipo.getDescricao(),
                justificativa);
    }
}
