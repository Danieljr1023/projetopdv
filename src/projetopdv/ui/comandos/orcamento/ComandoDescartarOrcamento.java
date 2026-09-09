package projetopdv.ui.comandos.orcamento;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;

import projetopdv.usuario.Permissao;

public class ComandoDescartarOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoDescartarOrcamento(SessaoOrcamento sessao) {
        super(
                "/descartar",
                """
                Cancela e descarta um orçamento (ABERTO ou CONFIRMADO), podendo ser recuperado posteriormente com /recuperar.

                Usos:
                /descartar                  (Descarta o orçamento ativo ou lista os disponíveis para descarte)
                /descartar <ID ou Nome>     (Descarta o orçamento indicado diretamente após confirmação)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_CANCELAR)) {
            return true;
        }

        try {
            // Caso 1: Sem argumentos
            if (argumentos.length == 0) {
                // Se tem orçamento ativo, descarta o ativo
                if (sessao.temOrcamentoAtivo()) {
                    sessao.descartarOrcamento(sessao.getIdOrcamentoAtivo());
                    return true;
                }

                // Se não tem orçamento ativo, lista CONFIRMADOS e ABERTOS com seleção rápida
                List<Orcamento> disponiveis = new ArrayList<>();
                disponiveis.addAll(orcamentoDAO.listarPorStatus(StatusOrcamento.CONFIRMADO));
                disponiveis.addAll(orcamentoDAO.listarPorStatus(StatusOrcamento.ABERTO));

                if (disponiveis.isEmpty()) {
                    System.out.println("\nNenhum orçamento com status CONFIRMADO ou ABERTO encontrado para descarte.");
                    return true;
                }

                System.out.println("\n====================================================================================================");
                System.out.println("                       ORÇAMENTOS DISPONÍVEIS PARA DESCARTE / CANCELAMENTO                          ");
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
                System.out.println("Digite o número da opção desejada para descartar (ou digite outro comando):");

                sessao.definirOpcoesDescartarPendentes(disponiveis);
                return true;
            }

            // Caso 2: Com argumentos (ID ou Nome)
            String termo = String.join(" ", argumentos).trim();
            if (termo.toLowerCase().startsWith("id ")) {
                termo = termo.substring(3).trim();
            } else if (termo.toLowerCase().startsWith("nome ")) {
                termo = termo.substring(5).trim();
            }

            // Tenta por ID numérico ou com prefixo '#'
            Integer idPesquisa = null;
            try {
                idPesquisa = Integer.valueOf(termo);
            } catch (NumberFormatException e) {
                if (termo.startsWith("#")) {
                    try {
                        idPesquisa = Integer.valueOf(termo.substring(1).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Se for ID, verifica se o orçamento existe
            if (idPesquisa != null) {
                Orcamento o = orcamentoDAO.buscarPorId(idPesquisa);
                if (o != null) {
                    sessao.descartarOrcamento(o.getIdOrcamento());
                    return true;
                }
            }

            // Tenta por nome (estritamente CONFIRMADO ou ABERTO)
            List<Orcamento> porNome = orcamentoDAO.buscarPorNome(termo).stream()
                    .filter(o -> o.getStatusOrcamento() == StatusOrcamento.CONFIRMADO || o.getStatusOrcamento() == StatusOrcamento.ABERTO)
                    .toList();

            if (porNome.size() == 1) {
                sessao.descartarOrcamento(porNome.get(0).getIdOrcamento());
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
                System.out.println("Digite o número da opção desejada para descartar:");
                sessao.definirOpcoesDescartarPendentes(porNome);
            } else {
                System.out.println("Orçamento não encontrado.");
            }

        } catch (SQLException e) {
            System.out.println("Erro ao acessar banco de dados: " + e.getMessage());
        }

        return true;
    }
}

