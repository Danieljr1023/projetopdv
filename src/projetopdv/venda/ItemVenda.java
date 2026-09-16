package projetopdv.venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.produto.Produto;

public final class ItemVenda {

    private final int idItemVenda;
    private final int numeroItem;
    private final Integer numeroItemOrcamento;
    private final Produto produto;
    private final int idProduto;
    private final String nomeProduto;
    private final String codigoBarras;
    private final int quantidade;
    private final BigDecimal precoUnitarioTabela;
    private final BigDecimal precoUnitario;
    private int quantidadeDevolvida;

    public ItemVenda(int idItemVenda, int numeroItem, Integer numeroItemOrcamento, Produto produto, int idProduto,
                     String nomeProduto, String codigoBarras, int quantidade, BigDecimal precoUnitarioTabela,
                     BigDecimal precoUnitario, int quantidadeDevolvida) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário não pode ser nulo ou negativo.");
        }
        if (precoUnitarioTabela == null || precoUnitarioTabela.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário de tabela não pode ser nulo ou negativo.");
        }
        if (quantidadeDevolvida < 0 || quantidadeDevolvida > quantidade) {
            throw new IllegalArgumentException("Quantidade devolvida inválida.");
        }

        this.idItemVenda = idItemVenda;
        this.numeroItem = numeroItem > 0 ? numeroItem : 1;
        this.numeroItemOrcamento = (numeroItemOrcamento != null && numeroItemOrcamento > 0) ? numeroItemOrcamento : null;
        this.produto = produto;
        this.idProduto = idProduto > 0 ? idProduto : (produto != null ? produto.getIdProduto() : 0);
        this.nomeProduto = (nomeProduto != null && !nomeProduto.isBlank())
                ? nomeProduto.trim()
                : (produto != null ? produto.getNomeProduto() : "Produto Sem Nome");
        this.codigoBarras = (codigoBarras != null && !codigoBarras.isBlank())
                ? codigoBarras.trim()
                : (produto != null ? produto.getCodBarras() : "");
        this.quantidade = quantidade;
        this.precoUnitarioTabela = precoUnitarioTabela.setScale(2, RoundingMode.HALF_UP);
        this.precoUnitario = precoUnitario.setScale(2, RoundingMode.HALF_UP);
        this.quantidadeDevolvida = quantidadeDevolvida;
    }

    public ItemVenda(int idItemVenda, Produto produto, int quantidade, BigDecimal precoUnitario) {
        this(
                idItemVenda,
                1,
                null,
                produto,
                produto != null ? produto.getIdProduto() : 0,
                produto != null ? produto.getNomeProduto() : "",
                produto != null ? produto.getCodBarras() : "",
                quantidade,
                (produto != null && produto.getPrecoVenda() != null) ? produto.getPrecoVenda() : precoUnitario,
                precoUnitario,
                0
        );
    }

    public ItemVenda(Produto produto, int quantidade, BigDecimal precoUnitario) {
        this(0, produto, quantidade, precoUnitario);
    }

    public ItemVenda(Produto produto, int quantidade) {
        this(0, produto, quantidade, produto != null ? produto.getPrecoVenda() : BigDecimal.ZERO);
    }

    public static ItemVenda deItemOrcamento(int numeroItemVenda, ItemOrcamento item) {
        if (item == null) {
            throw new IllegalArgumentException("Item de orçamento não pode ser nulo.");
        }
        return new ItemVenda(
                0,
                numeroItemVenda,
                item.getNumeroItem(),
                item.getProduto(),
                item.getIdProduto(),
                item.getNomeProduto(),
                item.getCodigoBarras(),
                item.getQuantidade(),
                item.getPrecoUnitarioTabela(),
                item.getPrecoUnitarioLiquido(),
                0
        );
    }

    public int getIdItemVenda() {
        return idItemVenda;
    }

    public int getNumeroItem() {
        return numeroItem;
    }

    public Integer getNumeroItemOrcamento() {
        return numeroItemOrcamento;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getIdProduto() {
        return idProduto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitarioTabela() {
        return precoUnitarioTabela;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getValorSubtotalBruto() {
        return this.precoUnitarioTabela.multiply(BigDecimal.valueOf(this.quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorItem() {
        return this.precoUnitario.multiply(BigDecimal.valueOf(this.quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorTotal() {
        return getValorItem();
    }

    public BigDecimal getValorDescontoUnitario() {
        return this.precoUnitarioTabela.subtract(this.precoUnitario).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorDescontoTotal() {
        return getValorSubtotalBruto().subtract(getValorTotal()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean temDesconto() {
        return this.precoUnitario.compareTo(this.precoUnitarioTabela) < 0;
    }

    public BigDecimal getPercentualDesconto() {
        BigDecimal bruto = getValorSubtotalBruto();
        if (bruto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getValorDescontoTotal()
                .multiply(BigDecimal.valueOf(100))
                .divide(bruto, 2, RoundingMode.HALF_UP);
    }

    public int getQuantidadeDevolvida() {
        return quantidadeDevolvida;
    }

    public int getQuantidadeDisponivelParaDevolucao() {
        return Math.max(0, quantidade - quantidadeDevolvida);
    }

    public boolean podeDevolver(int qtd) {
        return qtd > 0 && qtd <= getQuantidadeDisponivelParaDevolucao();
    }

    public void registrarDevolucao(int qtd) {
        if (!podeDevolver(qtd)) {
            throw new IllegalArgumentException(String.format(
                    "Quantidade de devolução inválida (%d). Disponível para devolução: %d.",
                    qtd, getQuantidadeDisponivelParaDevolucao()
            ));
        }
        this.quantidadeDevolvida += qtd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemVenda itemVenda = (ItemVenda) o;
        if (idItemVenda > 0 && itemVenda.idItemVenda > 0) {
            return idItemVenda == itemVenda.idItemVenda;
        }
        return numeroItem == itemVenda.numeroItem && idProduto == itemVenda.idProduto;
    }

    @Override
    public int hashCode() {
        return idItemVenda > 0 ? Objects.hash(idItemVenda) : Objects.hash(numeroItem, idProduto);
    }

    @Override
    public String toString() {
        if (temDesconto()) {
            return String.format("%s (x%d) - R$ %.2f (Desconto de R$ %.2f)",
                    nomeProduto, quantidade, getValorItem(), getValorDescontoTotal());
        }
        return String.format("%s (x%d) - R$ %.2f",
                nomeProduto, quantidade, getValorItem());
    }
}
