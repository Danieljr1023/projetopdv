package projetopdv.ui.comandos.orcamento;

import projetopdv.orcamento.Orcamento;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;

import projetopdv.usuario.Permissao;

public class ComandoResumoOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoResumoOrcamento(SessaoOrcamento sessao) {
        super(
                "/resumo",
                """
                Exibe o resumo detalhado em formato de cupom do orçamento ativo.

                Uso:
                /resumo
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_CONSULTAR)) {
            return true;
        }

        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento.");
            return true;
        }

        Orcamento orc = sessao.getOrcamentoAtivo();
        if (orc == null) {
            System.out.println("Orçamento ativo não encontrado.");
            return true;
        }

        sessao.imprimirResumoCompleto(orc);
        return true;
    }
}
