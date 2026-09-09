package projetopdv.ui.comandos;

public class ComandoHelp extends ComandoPainel {

    private final ComandosPainel comandos;

    public ComandoHelp(ComandosPainel comandos) {

        super(
                "/help",
                """
                        Lista comandos ou mostra detalhes de um comando.
                        /help lista todos os comandos existentes.
                        Use /help (nome de um comando) para ver detalhes dele.
                """
        );

        this.comandos = comandos;
    }

    @Override
    public boolean executar(String[] argumentos) {

        if (argumentos.length > 1) {
            System.out.println("Uso correto:");
            System.out.println("/help");
            System.out.println("/help <comando>");
            return true;
        }

        if (argumentos.length == 0) {

            System.out.println("\nComandos disponíveis:");

            for (ComandoPainel comando : comandos.listar()) {
                System.out.println(comando.getNome());
            }

            return true;
        }

        String nomeComando = argumentos[0];

        if (!nomeComando.startsWith("/")) {
            nomeComando = "/" + nomeComando;
        }

        ComandoPainel comando = comandos.buscar(nomeComando);

        if (comando == null) {

            System.out.println(
                    "Comando não encontrado: " + nomeComando
            );

            return true;
        }

        System.out.println();
        System.out.println("Comando: " + comando.getNome());
        System.out.println("Descrição:");
        System.out.println(comando.getDescricao());

        return true;
    }
}
