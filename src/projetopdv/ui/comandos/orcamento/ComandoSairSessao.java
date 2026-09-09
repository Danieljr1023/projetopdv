package projetopdv.ui.comandos.orcamento;

import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;

public class ComandoSairSessao extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoSairSessao(SessaoOrcamento sessao) {
        super(
                "/sair",
                """
                Sai da sessão do orçamento ativo mantendo-o salvo no estado atual.

                Uso:
                /sair
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento.");
            return true;
        }

        sessao.sairOrcamento();
        return true;
    }
}
