package projetopdv.ui.comandos.caixa;

import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;

public class ComandoLogoutCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoLogoutCaixa(SessaoCaixa sessao) {
        super("/logout", "Desconecta o operador de caixa atual.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.estaAutenticado()) {
            System.out.println("Nenhum operador conectado.");
            return true;
        }

        String nome = sessao.getUsuarioLogado().getNomeUsuario();
        sessao.setUsuarioLogado(null);
        System.out.printf("Operador '%s' desconectado com sucesso.%n", nome);
        return true;
    }
}
