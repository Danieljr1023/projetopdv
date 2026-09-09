package projetopdv.ui.comandos.orcamento;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoConfirmarOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;
    private final Scanner entrada;

    public ComandoConfirmarOrcamento(SessaoOrcamento sessao) {
        super(
                "/confirmar",
                """
                Confirma o orçamento ativo para envio ao faturamento/caixa.
                Exige que o orçamento possua nome e solicita autorização caso haja descontos.

                Uso:
                /confirmar
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
        this.entrada = sessao.getEntrada();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento.");
            return true;
        }

        Orcamento orc = sessao.getOrcamentoAtivo();
        if (orc == null) {
            System.out.println("Orçamento ativo não encontrado.");
            return true;
        }

        if (orc.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            System.out.println("Apenas orçamentos com status ABERTO podem ser confirmados.");
            return true;
        }

        String nome;
        try {
            nome = orc.getNomeOrcamento();
        } catch (Exception e) {
            nome = "";
        }

        // 1. Validar e solicitar nome caso esteja sem nome
        while (nome.isEmpty()) {
            System.out.println("\n[NOME OBRIGATÓRIO]");
            System.out.println("Para confirmar o orçamento, é obrigatório definir um nome de identificação.");
            System.out.print("Digite o nome para o orçamento (ou 'cancelar' para abortar): ");
            String inputNome = entrada.nextLine().trim();

            if (inputNome.equalsIgnoreCase("cancelar")) {
                System.out.println("Confirmação de orçamento cancelada.");
                return true;
            }

            if (inputNome.isEmpty()) {
                System.out.println("Nome não pode ser vazio.");
                continue;
            }

            if (inputNome.contains("%")) {
                System.out.println("O nome do orçamento não pode conter o caractere '%'.");
                continue;
            }

            try {
                if (orcamentoDAO.nomeConfirmadoJaExiste(inputNome, orc.getIdOrcamento())) {
                    System.out.println("Já existe outro orçamento CONFIRMADO com o nome '" + inputNome + "'. Escolha outro nome.");
                    continue;
                }
            } catch (SQLException e) {
                System.out.println("Erro ao checar nomes existentes: " + e.getMessage());
            }

            nome = inputNome;
        }

        // 2. Verificar se o nome já existe em outro orçamento CONFIRMADO
        try {
            if (orcamentoDAO.nomeConfirmadoJaExiste(nome, orc.getIdOrcamento())) {
                System.out.println("\n[ERRO DE UNICIDADE]");
                System.out.println("Já existe outro orçamento CONFIRMADO com o nome '" + nome + "'.");
                System.out.print("Digite um novo nome para este orçamento (ou 'cancelar'): ");
                String novoNome = entrada.nextLine().trim();
                if (novoNome.equalsIgnoreCase("cancelar") || novoNome.isEmpty()) {
                    System.out.println("Confirmação cancelada.");
                    return true;
                }
                nome = novoNome;
            }
        } catch (SQLException e) {
            System.out.println("Erro ao validar nome: " + e.getMessage());
        }

        // 3. Verificar se há itens com desconto aplicado
        List<ItemOrcamento> itensComDesconto = new ArrayList<>();
        for (ItemOrcamento item : orc.getItensOrcamento()) {
            if (item.temDesconto()) {
                itensComDesconto.add(item);
            }
        }

        if (!itensComDesconto.isEmpty()) {
            if (!sessao.validarPermissao(Permissao.ORCAMENTO_APROVAR_DESCONTO)) {
                return true;
            }

            System.out.println("\n================================================================================");
            System.out.println("                    AUTORIZAÇÃO DE DESCONTOS NECESSÁRIA                         ");
            System.out.println("================================================================================");
            for (ItemOrcamento item : itensComDesconto) {
                System.out.println(String.format("  Item %02d: %-30s | Tabela: R$ %7.2f -> Praticado: R$ %7.2f (-R$ %.2f / %.2f%%)",
                        item.getNumeroItem(),
                        item.getNomeProduto(),
                        item.getPrecoUnitarioTabela(),
                        item.getPrecoUnitarioLiquido(),
                        item.getValorDescontoUnitario(),
                        item.getPercentualDesconto()));
            }
            System.out.println("--------------------------------------------------------------------------------");
            System.out.print("Deseja autorizar os descontos e confirmar o orçamento? (s/n): ");
            String resposta = entrada.nextLine().trim().toLowerCase();

            if (!resposta.equals("s") && !resposta.equals("sim")) {
                System.out.println("Confirmação de orçamento abortada pelo operador.");
                return true;
            }
        } else {
            if (!sessao.validarPermissao(Permissao.ORCAMENTO_CRIAR)) {
                return true;
            }
        }

        // 4. Efetivar confirmação no SQLite
        try {
            boolean confirmou = orcamentoDAO.confirmarOrcamento(orc.getIdOrcamento(), nome);
            if (confirmou) {
                Orcamento orcFinal = orcamentoDAO.buscarPorId(orc.getIdOrcamento());
                System.out.println("\n================================================================================");
                System.out.println(String.format("                ORÇAMENTO #%04d CONFIRMADO COM SUCESSO!                 ", orcFinal.getIdOrcamento()));
                System.out.println("================================================================================");
                System.out.println(String.format(" Nome: %-30s Status: %s", orcFinal.getNomeOrcamento(), orcFinal.getStatusOrcamento()));
                System.out.println(String.format(" Total: R$ %-27.2f Itens: %d", orcFinal.getValorTotal(), orcFinal.getItensOrcamento().size()));
                System.out.println(" Orçamento disponível no caixa para faturamento. Retornando ao menu principal...");
                System.out.println("================================================================================");

                sessao.sairOrcamento();
            } else {
                System.out.println("Não foi possível confirmar o orçamento.");
            }
        } catch (SQLException | IllegalStateException | IllegalArgumentException e) {
            System.out.println("Erro ao confirmar orçamento: " + e.getMessage());
        }

        return true;
    }
}
