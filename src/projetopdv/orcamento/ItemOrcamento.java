package projetopdv.orcamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.produto.Produto;

public final class ItemOrcamento {

    private int numeroItem;
    private Orcamento orcamento;
    private final Produto produto;
    private String nomeProduto;
    private String codigoBarras;
    private final BigDecimal precoUnitarioTabela;
    private BigDecimal precoUnitarioLiquido;
    private int quantidade;

    public ItemOrcamento(int numeroItem, Produto produto, String nomeProduto, String codigoBarras, BigDecimal precoUnitarioTabela, BigDecimal precoUnitarioLiquido, int quantidade) {
        if (numeroItem <= 0) {
            throw new IllegalArgumentException("O número do item deve ser maior que zero.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (precoUnitarioTabela == null || precoUnitarioTabela.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário de tabela não pode ser nulo ou negativo.");
        }
        if (precoUnitarioLiquido == null || precoUnitarioLiquido.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário líquido não pode ser nulo ou negativo.");
        }
        this.numeroItem = numeroItem;
        this.produto = produto;
        this.nomeProduto = nomeProduto != null ? nomeProduto.trim() : (produto != null ? produto.getNomeProduto() : "");
        this.codigoBarras = codigoBarras != null ? codigoBarras.trim() : (produto != null ? produto.getCodBarras() : "");
        this.precoUnitarioTabela = precoUnitarioTabela.setScale(2, RoundingMode.HALF_UP);
        this.precoUnitarioLiquido = precoUnitarioLiquido.setScale(2, RoundingMode.HALF_UP);
        this.quantidade = quantidade;
    }

    public ItemOrcamento(int numeroItem, Produto produto, String nomeProduto, String codigoBarras, BigDecimal precoUnitarioTabela, int quantidade) {
        this(numeroItem, produto, nomeProduto, codigoBarras, precoUnitarioTabela, precoUnitarioTabela, quantidade);
    }

    public ItemOrcamento(int numeroItem, int idProduto, String nomeProduto, String codigoBarras, BigDecimal precoUnitarioTabela, BigDecimal precoUnitarioLiquido, int quantidade) {
        this(numeroItem, (Produto) null, nomeProduto, codigoBarras, precoUnitarioTabela, precoUnitarioLiquido, quantidade);
    }

    public ItemOrcamento(int numeroItem, int idProduto, String nomeProduto, String codigoBarras, BigDecimal precoUnitarioTabela, int quantidade) {
        this(numeroItem, (Produto) null, nomeProduto, codigoBarras, precoUnitarioTabela, precoUnitarioTabela, quantidade);
    }

    public int getNumeroItem() {
        return numeroItem;
    }

    public void setNumeroItem(int numeroItem) {
        verificarPermissaoAlterarDadosCadastrais();
        if (numeroItem <= 0) {
            throw new IllegalArgumentException("O número do item deve ser maior que zero.");
        }
        this.numeroItem = numeroItem;
    }

    public Orcamento getOrcamento() {
        return orcamento;
    }

    void vincularOrcamento(Orcamento orcamento) {
        if (this.orcamento != null && this.orcamento != orcamento) {
            throw new IllegalStateException("O item já pertence a um orçamento e não pode ser transferido.");
        }
        this.orcamento = orcamento;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getIdProduto() {
        return produto != null ? produto.getIdProduto() : 0;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void atualizarDadosDoProduto() {
        verificarPermissaoAlterarDadosCadastrais();
        if (this.produto != null) {
            this.nomeProduto = this.produto.getNomeProduto();
            this.codigoBarras = this.produto.getCodBarras();
        }
    }

    public BigDecimal getPrecoUnitarioTabela() {
        return precoUnitarioTabela;
    }

    public BigDecimal getPrecoUnitarioLiquido() {
        return precoUnitarioLiquido;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitarioLiquido;
    }

    public BigDecimal getValorDescontoUnitario() {
        BigDecimal diferenca = precoUnitarioTabela.subtract(precoUnitarioLiquido);
        return diferenca.compareTo(BigDecimal.ZERO) > 0 ? diferenca : BigDecimal.ZERO;
    }

    public BigDecimal getPercentualDesconto() {
        if (precoUnitarioTabela.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal diferenca = precoUnitarioTabela.subtract(precoUnitarioLiquido);
        if (diferenca.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return diferenca.multiply(new BigDecimal("100"))
                .divide(precoUnitarioTabela, 2, RoundingMode.HALF_UP);
    }

    public boolean temDesconto() {
        return precoUnitarioLiquido.compareTo(precoUnitarioTabela) < 0;
    }

    public void setPrecoUnitarioLiquido(BigDecimal precoUnitarioLiquido) {
        verificarPermissaoAlterarValor();
        if (precoUnitarioLiquido == null || precoUnitarioLiquido.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário líquido não pode ser nulo ou negativo.");
        }
        this.precoUnitarioLiquido = precoUnitarioLiquido.setScale(2, RoundingMode.HALF_UP);
        if (this.orcamento != null) {
            this.orcamento.recalcularTotal();
        }
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        setPrecoUnitarioLiquido(precoUnitario);
    }

    public void aplicarDescontoPercentual(BigDecimal percentual) {
        verificarPermissaoAlterarValor();
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) < 0 || percentual.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentual de desconto deve estar entre 0 e 100.");
        }
        BigDecimal fator = BigDecimal.ONE.subtract(percentual.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        setPrecoUnitarioLiquido(this.precoUnitarioTabela.multiply(fator).setScale(2, RoundingMode.HALF_UP));
    }

    public void aplicarDescontoValor(BigDecimal valorDesconto) {
        verificarPermissaoAlterarValor();
        if (valorDesconto == null || valorDesconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor de desconto inválido.");
        }
        if (valorDesconto.compareTo(this.precoUnitarioTabela) > 0) {
            throw new IllegalArgumentException("O desconto não pode ser maior que o preço unitário de tabela.");
        }
        setPrecoUnitarioLiquido(this.precoUnitarioTabela.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP));
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        verificarPermissaoAlterarQuantidade();
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        this.quantidade = quantidade;
        if (this.orcamento != null) {
            this.orcamento.recalcularTotal();
        }
    }

    public BigDecimal getValorBruto() {
        return this.precoUnitarioTabela.multiply(BigDecimal.valueOf(this.quantidade));
    }

    public BigDecimal getValorItem() {
        return this.precoUnitarioLiquido.multiply(BigDecimal.valueOf(this.quantidade));
    }

    public BigDecimal getValorLiquido() {
        return getValorItem();
    }

    public BigDecimal getDescontoTotal() {
        BigDecimal desc = getValorBruto().subtract(getValorLiquido());
        return desc.compareTo(BigDecimal.ZERO) > 0 ? desc : BigDecimal.ZERO;
    }

    public BigDecimal getValorSubtotalBruto() {
        return getValorBruto();
    }

    public BigDecimal getValorDescontoTotal() {
        return getDescontoTotal();
    }

    private void verificarPermissaoAlterarDadosCadastrais() {
        if (orcamento != null && orcamento.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            throw new IllegalStateException(
                    "Nome, código de barras e número do item só podem ser alterados quando o orçamento estiver ABERTO."
            );
        }
    }

    private void verificarPermissaoAlterarValor() {
        if (orcamento != null) {
            StatusOrcamento status = orcamento.getStatusOrcamento();
            if (status != StatusOrcamento.ABERTO && status != StatusOrcamento.FATURANDO) {
                throw new IllegalStateException(
                        "O valor do item não pode ser alterado quando o orçamento estiver " + status + "."
                );
            }
        }
    }

    private void verificarPermissaoAlterarQuantidade() {
        if (orcamento != null && orcamento.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            throw new IllegalStateException(
                    "A quantidade do item só pode ser alterada quando o orçamento estiver ABERTO."
            );
        }
    }

    @Override
    public String toString() {
        if (temDesconto()) {
            return String.format("Item %02d: %s (x%d) - Tabela: R$ %.2f | Líquido: R$ %.2f (%.2f%% OFF) - Total: R$ %.2f",
                    numeroItem,
                    nomeProduto.isEmpty() ? "Produto #" + getIdProduto() : nomeProduto,
                    quantidade,
                    precoUnitarioTabela,
                    precoUnitarioLiquido,
                    getPercentualDesconto(),
                    getValorItem());
        }
        return String.format("Item %02d: %s (x%d) - R$ %.2f - Total: R$ %.2f",
                numeroItem,
                nomeProduto.isEmpty() ? "Produto #" + getIdProduto() : nomeProduto,
                quantidade,
                precoUnitarioLiquido,
                getValorItem());
    }
}
