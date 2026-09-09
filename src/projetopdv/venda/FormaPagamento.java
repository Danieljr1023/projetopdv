package projetopdv.venda;

public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    VALE_COMPRA("Vale-Compra"),
    OUTRO("Outro");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static FormaPagamento fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return OUTRO;
        }
        String t = texto.trim();
        for (FormaPagamento f : values()) {
            if (f.name().equalsIgnoreCase(t) || f.descricao.equalsIgnoreCase(t)) {
                return f;
            }
        }
        return OUTRO;
    }
}
