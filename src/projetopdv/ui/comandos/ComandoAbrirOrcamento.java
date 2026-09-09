package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoAbrirOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoAbrirOrcamento(SessaoOrcamento sessao) {
        super(
                "/abrir",
                """
                Abre um orçamento (CONFIRMADO ou ABERTO) para alterações ou lista os disponíveis.

                Usos:
                /abrir               (Lista orçamentos CONFIRMADOS e ABERTOS com seleção rápida)
                /abrir <ID ou Nome>  (Abre o orçamento diretamente)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_CONSULTAR)) {
            return true;
        }

        try {
            if (argumentos.length == 0) {
                // Listar orçamentos CONFIRMADOS e ABERTOS com opções numeradas
                List<Orcamento> disponiveis = new ArrayList<>();
                disponiveis.addAll(orcamentoDAO.listarPorStatus(StatusOrcamento.CONFIRMADO));
                disponiveis.addAll(orcamentoDAO.listarPorStatus(StatusOrcamento.ABERTO));

                if (sessao.temOrcamentoAtivo()) {
                    disponiveis = disponiveis.stream()
                            .filter(o -> o.getIdOrcamento() != sessao.getIdOrcamentoAtivo())
                            .toList();
                }

                if (disponiveis.isEmpty()) {
                    System.out.println("\nNenhum outro orçamento com status CONFIRMADO ou ABERTO encontrado para abertura.");
                    return true;
                }

                System.out.println("\n====================================================================================================");
                System.out.println("                       ORÇAMENTOS DISPONÍVEIS PARA ABERTURA / EDIÇÃO                                ");
                System.out.println("====================================================================================================");
                for (int i = 0; i < disponiveis.size(); i++) {
                    Orcamento o = disponiveis.get(i);
                    String clienteStr = o.temCliente() ? o.getCliente().getNomeCliente() : "Não informado";
                    if (clienteStr.length() > 18) {
                        clienteStr = clienteStr.substring(0, 15) + "...";
                    }
                    String nome;
                    try {
                        nome = o.getNomeOrcamento();
                    } catch (Exception e) {
                        nome = "Sem Nome";
                    }
                    if (nome.length() > 22) {
                        nome = nome.substring(0, 19) + "...";
                    }
                    System.out.println(String.format("[%d] ID: %-4d | Nome: %-22s | Status: %-11s | Cliente: %-18s | Total: R$ %9.2f",
                            (i + 1), o.getIdOrcamento(), nome, o.getStatusOrcamento(), clienteStr, o.getValorTotal()));
                }
                System.out.println("====================================================================================================");
                System.out.println("Digite o número da opção desejada para abrir (ou digite outro comando):");

                sessao.definirOpcoesAbrirPendentes(disponiveis);
                return true;
            }

            String termo = String.join(" ", argumentos).trim();
            if (termo.toLowerCase().startsWith("id ")) {
                termo = termo.substring(3).trim();
            } else if (termo.toLowerCase().startsWith("nome ")) {
                termo = termo.substring(5).trim();
            }

            // Tenta por ID (CONFIRMADO ou ABERTO)
            try {
                int id = Integer.parseInt(termo);
                Orcamento o = orcamentoDAO.buscarPorId(id);
                if (o != null && (o.getStatusOrcamento() == StatusOrcamento.CONFIRMADO || o.getStatusOrcamento() == StatusOrcamento.ABERTO)) {
                    sessao.abrirOrcamento(id);
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }

            // Tenta por nome (CONFIRMADO ou ABERTO)
            List<Orcamento> porNome = orcamentoDAO.buscarPorNome(termo).stream()
                    .filter(o -> o.getStatusOrcamento() == StatusOrcamento.CONFIRMADO || o.getStatusOrcamento() == StatusOrcamento.ABERTO)
                    .toList();

            if (porNome.size() == 1) {
                sessao.abrirOrcamento(porNome.get(0).getIdOrcamento());
            } else if (porNome.size() > 1) {
                System.out.println("\nForam encontrados " + porNome.size() + " orçamentos para '" + termo + "':");
                for (int i = 0; i < porNome.size(); i++) {
                    Orcamento o = porNome.get(i);
                    String nome;
                    try {
                        nome = o.getNomeOrcamento();
                    } catch (Exception e) {
                        nome = "Sem Nome";
                    }
                    System.out.println(String.format("[%d] ID: %-4d | Nome: %-22s | Status: %-11s | Total: R$ %9.2f",
                            (i + 1), o.getIdOrcamento(), nome, o.getStatusOrcamento(), o.getValorTotal()));
                }
                System.out.println("Digite o número da opção desejada para abrir:");
                sessao.definirOpcoesAbrirPendentes(porNome);
            } else {
                System.out.println("Orçamento não encontrado.");
            }

        } catch (SQLException e) {
            System.out.println("Erro ao acessar banco de dados: " + e.getMessage());
        }

        return true;
    }
}
