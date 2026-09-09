package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.caixa.ResumoTurnoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoFecharCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoFecharCaixa(SessaoCaixa sessao) {
        super("/fechar_caixa", "Fecha o turno do caixa físico, realiza conferência cega e emite o relatório de fechamento.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.VENDA_FATURAR)) {
            return true;
        }

        if (!sessao.getCaixaDAO().isCaixaAberto()) {
            System.out.println("[AVISO] O caixa já se encontra fechado.");
            return true;
        }

        BigDecimal valorFisico = null;
        String motivo = null;

        if (argumentos.length > 0) {
            try {
                valorFisico = new BigDecimal(argumentos[0].replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor informado inválido. Exemplo: /fechar_caixa 450.00");
                return true;
            }
            if (argumentos.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < argumentos.length; i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(argumentos[i]);
                }
                motivo = sb.toString();
            }
        }

        if (valorFisico == null) {
            System.out.println("\n--- CONFERÊNCIA CEGA DE CAIXA ---");
            System.out.print("Informe o total físico em dinheiro contado na gaveta: R$ ");
            String input = sessao.getEntrada().nextLine().trim().replace(",", ".");
            if (input.isEmpty()) {
                valorFisico = BigDecimal.ZERO;
            } else {
                try {
                    valorFisico = new BigDecimal(input);
                } catch (NumberFormatException e) {
                    System.out.println("Valor inválido.");
                    return true;
                }
            }
        }

        if (valorFisico.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("O valor físico contado não pode ser negativo.");
            return true;
        }

        if (motivo == null || motivo.isBlank()) {
            System.out.print("Observações do fechamento de turno (opcional): ");
            motivo = sessao.getEntrada().nextLine().trim();
            if (motivo.isBlank()) {
                motivo = "Fechamento de turno por " + sessao.getUsuarioLogado().getNomeUsuario();
            }
        }

        // Obter resumo consolidado antes do fechamento
        ResumoTurnoCaixa resumo = sessao.getCaixaDAO().obterResumoTurnoAtual();

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarFechamentoCaixa(valorFisico, motivo, idOperador);

        if (mov != null) {
            System.out.println();
            System.out.print(resumo.gerarRelatorioFechamento(valorFisico));
            System.out.println("\n[✓] CAIXA FÍSICO FECHADO COM SUCESSO!");
            System.out.println("O terminal está agora bloqueado para novas vendas até que uma nova abertura seja efetuada.");
        } else {
            System.out.println("[ERRO] Falha ao registrar fechamento do caixa no banco de dados.");
        }

        return true;
    }
}
