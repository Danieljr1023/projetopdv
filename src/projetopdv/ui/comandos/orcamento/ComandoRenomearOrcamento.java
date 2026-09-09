package projetopdv.ui.comandos.orcamento;

import java.sql.SQLException;
import java.util.Scanner;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoRenomearOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoRenomearOrcamento(SessaoOrcamento sessao) {
        super(
                "/renomear",
                """
                Altera o nome do orçamento ativo (apenas em status ABERTO).

                Usos:
                /renomear                         (Modo interativo: exibe nome atual e solicita o novo)
                /renomear <Novo Nome do Orçamento> (Renomeação direta)
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

        Orcamento orc = sessao.getOrcamentoAtivo();
        if (orc == null || orc.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            System.out.println("Apenas orçamentos com status ABERTO podem ser renomeados.");
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        String novoNome;

        if (argumentos.length < 1) {
            String nomeAtual;
            try {
                nomeAtual = orc.getNomeOrcamento();
            } catch (Exception e) {
                nomeAtual = "Sem Nome";
            }
            System.out.printf("Nome atual do orçamento #%d: %s%n", orc.getIdOrcamento(), nomeAtual);
            System.out.print("Informe o novo nome do orçamento (ou 'cancelar'): ");
            novoNome = entrada.nextLine().trim();
            if (novoNome.isEmpty() || novoNome.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação cancelada.");
                return true;
            }
        } else {
            novoNome = String.join(" ", argumentos).trim();
        }
        if (novoNome.contains("%")) {
            System.out.println("O nome do orçamento não pode conter o caractere '%'.");
            return true;
        }

        try {
            String nomeAntigo;
            try {
                nomeAntigo = orc.getNomeOrcamento();
            } catch (Exception e) {
                nomeAntigo = "Sem Nome";
            }

            boolean alterou = orcamentoDAO.renomear(orc.getIdOrcamento(), novoNome);
            if (alterou) {
                System.out.println("\n[ORÇAMENTO RENOMEADO COM SUCESSO]");
                System.out.println(String.format("Orçamento #%d: '%s' -> '%s'", orc.getIdOrcamento(), nomeAntigo, novoNome));
            } else {
                System.out.println("Não foi possível renomear o orçamento.");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao renomear orçamento no banco: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }

        return true;
    }
}
