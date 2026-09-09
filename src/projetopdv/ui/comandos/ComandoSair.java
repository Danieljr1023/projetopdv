package projetopdv.ui.comandos;

public class ComandoSair extends ComandoPainel {

    public ComandoSair() {
        super(
                "/sair",
                "Encerra o sistema."
        );
    }

    @Override
    public boolean executar(String[] argumentos) {

        if (argumentos.length > 0) {
            System.out.println("Uso correto: /sair");
            return true;
        }

        System.out.println("Sistema encerrado.");
        return false;
    }
}
