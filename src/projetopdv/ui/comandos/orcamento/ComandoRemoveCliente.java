package projetopdv.ui.comandos.orcamento;

import java.sql.SQLException;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;

import projetopdv.usuario.Permissao;

public class ComandoRemoveCliente extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoRemoveCliente(SessaoOrcamento sessao) {
        super(
                "/remove_cliente",
                """
                Desvincula o cliente do orçamento ativo.

                Uso:
                /remove_cliente
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_EDITAR_ITENS)) {
            return true;
        }

        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento.");
            return true;
        }

        try {
            int idOrcamento = sessao.getIdOrcamentoAtivo();
            boolean desvinculado = orcamentoDAO.vincularCliente(idOrcamento, null);
            if (desvinculado) {
                System.out.println("\n[CLIENTE DESVINCULADO DO ORÇAMENTO]");
                System.out.println("O orçamento #" + idOrcamento + " agora não possui cliente vinculado.");
            } else {
                System.out.println("Não foi possível desvincular o cliente.");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao desvincular cliente no banco: " + e.getMessage());
        }

        return true;
    }
}
