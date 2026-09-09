package projetopdv.venda;

public enum ModalidadeEstorno {
    REEMBOLSO_IMEDIATO("Reembolso Imediato na Forma de Pagamento Original"),
    VALE_COMPRA("Conversão em Vale-Compra / Crédito de Loja");

    private final String descricao;

    ModalidadeEstorno(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static ModalidadeEstorno fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return REEMBOLSO_IMEDIATO;
        }
        for (ModalidadeEstorno m : values()) {
            if (m.name().equalsIgnoreCase(texto.trim()) || m.descricao.equalsIgnoreCase(texto.trim())) {
                return m;
            }
        }
        return REEMBOLSO_IMEDIATO;
    }
}
