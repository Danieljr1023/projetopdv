package projetopdv.ui.comandos;

import projetopdv.ui.SessaoOrcamento;

public class ComandoLogout extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoLogout(SessaoOrcamento sessao) {
        super(
                "/logout",
                """
                Encerra a sessão do operador atualmente autenticado.
                Retorna o sistema ao estado não autenticado.
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.isAutenticado()) {
            System.out.println("Nenhum usuário autenticado.");
            return true;
        }

        String nome = sessao.getUsuarioLogado().getNomeUsuario();
        sessao.deslogar();
        System.out.println("Logout realizado com sucesso. Até logo, " + nome + "!");
        return true;
    }
}
