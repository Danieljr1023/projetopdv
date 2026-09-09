package projetopdv.produto;

import java.math.BigDecimal;

public final class Produto {

    /// Um produto representa o produto atual cadastrado no sistema.
    
    private final int idProduto;
    private String codBarras;
    private String nomeProduto;
    private BigDecimal precoVenda;

    public Produto(int idProduto, String nomeProduto, String codBarras, BigDecimal precoVenda) {
        if (idProduto <= 0) {
            throw new IllegalArgumentException("ID do produto inválido.");
        }
        validarNome(nomeProduto);
        validarCodBarras(codBarras);
        validarPreco(precoVenda);

        this.idProduto = idProduto;
        this.nomeProduto = nomeProduto.trim();
        this.codBarras = codBarras != null ? codBarras.trim() : "";
        this.precoVenda = precoVenda;
    }

    // Getters e Setters
    public int getIdProduto() {
        return idProduto;
    }

    public String getCodBarras() {
        return codBarras;
    }

    public void setCodBarras(String codBarras) {
        validarCodBarras(codBarras);
        this.codBarras = codBarras != null ? codBarras.trim() : "";
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        validarNome(nomeProduto);
        this.nomeProduto = nomeProduto.trim();
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public BigDecimal getPreco() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        validarPreco(precoVenda);
        this.precoVenda = precoVenda;
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do produto não pode ser vazio.");
        }
        if (nome.contains("%")) {
            throw new IllegalArgumentException("O nome do produto não pode conter o caractere '%'.");
        }
    }

    private void validarCodBarras(String cod) {
        if (cod != null && cod.contains("%")) {
            throw new IllegalArgumentException("O código de barras não pode conter o caractere '%'.");
        }
    }

    private void validarPreco(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço de venda não pode ser nulo ou negativo.");
        }
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Nome: %s | Código de Barras: %s | Preço: R$ %.2f",
                idProduto, nomeProduto, codBarras, precoVenda);
    }
}
