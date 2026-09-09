package projetopdv.posvenda;

public enum StatusValeCompra {
    ATIVO("Ativo para uso"),
    UTILIZADO("Totalmente utilizado em compras"),
    RESGATADO_SANGRIA("Resgatado em dinheiro via sangria"),
    CANCELADO("Cancelado"),
    EXPIRADO("Expirado");

    private final String descricao;

    StatusValeCompra(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusValeCompra fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return ATIVO;
        }
        for (StatusValeCompra s : values()) {
            if (s.name().equalsIgnoreCase(texto.trim())) {
                return s;
            }
        }
        return ATIVO;
    }
}
