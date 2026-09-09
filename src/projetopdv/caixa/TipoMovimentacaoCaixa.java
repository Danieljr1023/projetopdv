package projetopdv.caixa;

public enum TipoMovimentacaoCaixa {
    ESTORNO_VENDA_DINHEIRO("Estorno Imediato de Venda em Dinheiro"),
    SANGRIA_RESGATE_VALE("Sangria por Resgate de Vale-Compra"),
    SANGRIA_OPERACIONAL("Sangria Operacional para Cofre"),
    SUPRIMENTO_TROCO("Suprimento de Fundo de Troco"),
    ABERTURA_CAIXA("Abertura de Caixa"),
    FECHAMENTO_CAIXA("Fechamento de Caixa");

    private final String descricao;

    TipoMovimentacaoCaixa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static TipoMovimentacaoCaixa fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return SANGRIA_OPERACIONAL;
        }
        for (TipoMovimentacaoCaixa tipo : values()) {
            if (tipo.name().equalsIgnoreCase(texto.trim())) {
                return tipo;
            }
        }
        return SANGRIA_OPERACIONAL;
    }
}
