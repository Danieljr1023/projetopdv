package projetopdv.posvenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ValeCompra {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final int idValeCompra;
    private final String codigoVale;
    private final Integer idCliente;
    private final Integer idVendaOrigem;
    private final BigDecimal valorOriginal;
    private BigDecimal saldo;
    private final LocalDateTime dataEmissao;
    private final LocalDateTime dataValidade;
    private StatusValeCompra statusVale;
    private String observacoes;

    public ValeCompra(int idValeCompra, String codigoVale, Integer idCliente, Integer idVendaOrigem,
                      BigDecimal valorOriginal, BigDecimal saldo, LocalDateTime dataEmissao,
                      LocalDateTime dataValidade, StatusValeCompra statusVale, String observacoes) {
        if (codigoVale == null || codigoVale.trim().isEmpty()) {
            throw new IllegalArgumentException("Código do vale-compra não pode ser vazio.");
        }
        if (valorOriginal == null || valorOriginal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor original do vale-compra deve ser maior que zero.");
        }
        if (saldo == null || saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Saldo do vale-compra não pode ser nulo ou negativo.");
        }
        if (saldo.compareTo(valorOriginal) > 0) {
            throw new IllegalArgumentException("Saldo não pode exceder o valor original.");
        }
        if (dataEmissao == null) {
            throw new IllegalArgumentException("Data de emissão não pode ser nula.");
        }

        this.idValeCompra = idValeCompra;
        this.codigoVale = codigoVale.trim().toUpperCase();
        this.idCliente = (idCliente != null && idCliente > 0) ? idCliente : null;
        this.idVendaOrigem = (idVendaOrigem != null && idVendaOrigem > 0) ? idVendaOrigem : null;
        this.valorOriginal = valorOriginal.setScale(2, RoundingMode.HALF_UP);
        this.saldo = saldo.setScale(2, RoundingMode.HALF_UP);
        this.dataEmissao = dataEmissao;
        this.dataValidade = dataValidade;
        this.statusVale = statusVale != null ? statusVale : StatusValeCompra.ATIVO;
        this.observacoes = observacoes != null ? observacoes.trim() : "";
    }

    public ValeCompra(int idValeCompra, String codigoVale, Integer idCliente, Integer idVendaOrigem,
                      BigDecimal valorOriginal, LocalDateTime dataEmissao, LocalDateTime dataValidade, String observacoes) {
        this(idValeCompra, codigoVale, idCliente, idVendaOrigem, valorOriginal, valorOriginal, dataEmissao, dataValidade, StatusValeCompra.ATIVO, observacoes);
    }

    public int getIdValeCompra() {
        return idValeCompra;
    }

    public String getCodigoVale() {
        return codigoVale;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public Integer getIdVendaOrigem() {
        return idVendaOrigem;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public LocalDateTime getDataValidade() {
        return dataValidade;
    }

    public StatusValeCompra getStatusVale() {
        return statusVale;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes != null ? observacoes.trim() : "";
    }

    public boolean estaExpirado() {
        return dataValidade != null && LocalDateTime.now().isAfter(dataValidade);
    }

    public boolean podeSerUsado() {
        return statusVale == StatusValeCompra.ATIVO
                && saldo.compareTo(BigDecimal.ZERO) > 0
                && !estaExpirado();
    }

    public void abaterSaldo(BigDecimal valorAbatido) {
        if (!podeSerUsado()) {
            throw new IllegalStateException("Vale-compra indisponível para uso (Status: " + statusVale + ").");
        }
        if (valorAbatido == null || valorAbatido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor a abater deve ser positivo.");
        }
        if (valorAbatido.compareTo(this.saldo) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Valor a abater (R$ %.2f) excede o saldo disponível (R$ %.2f).",
                    valorAbatido, this.saldo
            ));
        }

        this.saldo = this.saldo.subtract(valorAbatido).setScale(2, RoundingMode.HALF_UP);
        if (this.saldo.compareTo(BigDecimal.ZERO) == 0) {
            this.statusVale = StatusValeCompra.UTILIZADO;
        }
    }

    public void resgatarIntegralmente() {
        if (!podeSerUsado()) {
            throw new IllegalStateException("Vale-compra indisponível para resgate em dinheiro.");
        }
        this.saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.statusVale = StatusValeCompra.RESGATADO_SANGRIA;
    }

    public void cancelar() {
        if (this.statusVale == StatusValeCompra.UTILIZADO || this.statusVale == StatusValeCompra.RESGATADO_SANGRIA) {
            throw new IllegalStateException("Não é possível cancelar um vale-compra já liquidado.");
        }
        this.statusVale = StatusValeCompra.CANCELADO;
    }

    public String gerarComprovante() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("          VALE-COMPRA / CRÉDITO         \n");
        sb.append("========================================\n");
        sb.append(String.format("CÓDIGO:    %s\n", codigoVale));
        sb.append(String.format("EMISSÃO:   %s\n", dataEmissao.format(FORMATO_DATA_HORA)));
        if (dataValidade != null) {
            sb.append(String.format("VALIDADE:  %s\n", dataValidade.format(FORMATO_DATA_HORA)));
        }
        if (idCliente != null) {
            sb.append(String.format("CLIENTE:   ID #%d\n", idCliente));
        }
        if (idVendaOrigem != null) {
            sb.append(String.format("VENDA REF: #%d\n", idVendaOrigem));
        }
        sb.append("----------------------------------------\n");
        sb.append(String.format("VALOR ORIGINAL:   R$ %.2f\n", valorOriginal));
        sb.append(String.format("SALDO DISPONÍVEL: R$ %.2f\n", saldo));
        sb.append(String.format("SITUAÇÃO:         %s\n", statusVale.getDescricao()));
        if (!observacoes.isEmpty()) {
            sb.append(String.format("OBS: %s\n", observacoes));
        }
        sb.append("========================================\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValeCompra that = (ValeCompra) o;
        return idValeCompra == that.idValeCompra && idValeCompra > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idValeCompra);
    }

    @Override
    public String toString() {
        return String.format("[%s] Saldo: R$ %.2f / Total: R$ %.2f (%s)",
                codigoVale, saldo, valorOriginal, statusVale);
    }
}
