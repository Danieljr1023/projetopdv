package projetopdv.ui.comandos;

public abstract class ComandoPainel {

    private final String nome;
    private final String descricao;

    public ComandoPainel(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public abstract boolean executar(String[] argumentos);
}
