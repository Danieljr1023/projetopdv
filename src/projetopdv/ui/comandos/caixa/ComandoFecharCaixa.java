package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.caixa.ResumoTurnoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoFecharCaixa extends ComandoPainel {

    private static final int LIMITE_CARACTERES_JUSTIFICATIVA = 200;

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

        ResumoTurnoCaixa resumo = sessao.getCaixaDAO().obterResumoTurnoAtual();
        BigDecimal saldoEsperado = resumo.getSaldoEsperadoGaveta();

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

        // Loop de conferência e recontagem
        while (true) {
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
                        valorFisico = null;
                        continue;
                    }
                }
            }

            if (valorFisico.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("O valor físico contado não pode ser negativo.");
                valorFisico = null;
                continue;
            }

            BigDecimal diferenca = valorFisico.subtract(saldoEsperado);
            if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
                String tipoDif = diferenca.compareTo(BigDecimal.ZERO) > 0 ? "SOBRA DE CAIXA" : "QUEBRA DE CAIXA";

                System.out.println("\n========================================================================");
                System.out.println("     [!] ATENÇÃO: DIVERGÊNCIA CONSTATADA NO FECHAMENTO DE TURNO!        ");
                System.out.println("========================================================================");
                System.out.printf("SALDO ESPERADO NA GAVETA: R$ %10.2f%n", saldoEsperado);
                System.out.printf("VALOR FÍSICO INFORMADO:   R$ %10.2f%n", valorFisico);
                System.out.printf("DIFERENÇA APURADA:        %+10.2f (%s)%n", diferenca, tipoDif);
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Opções disponíveis:");
                System.out.println("  [C] Confirmar fechamento com divergência");
                System.out.println("  [R] Recontar o dinheiro da gaveta e informar novo valor");
                System.out.println("  [X] Cancelar fechamento (manter caixa aberto para verificação)");
                System.out.print("Escolha a opção (C/R/X): ");

                String opcao = sessao.getEntrada().nextLine().trim().toUpperCase();
                if (opcao.startsWith("R")) {
                    System.out.println("\n--- RECONTAGEM DE FECHAMENTO ---");
                    valorFisico = null;
                    continue;
                } else if (opcao.startsWith("X")) {
                    System.out.println("Fechamento de caixa cancelado pelo operador. O caixa permanece aberto.");
                    return true;
                } else if (!opcao.startsWith("C")) {
                    System.out.println("Opção inválida. Fechamento cancelado por segurança.");
                    return true;
                }
            }

            break;
        }

        if (valorFisico == null) {
            return true;
        }

        BigDecimal diferenca = valorFisico.subtract(saldoEsperado);
        String justFinal;
        if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
            String tipoDif = diferenca.compareTo(BigDecimal.ZERO) > 0 ? "SOBRA DE CAIXA" : "QUEBRA DE CAIXA";

            if (motivo == null || motivo.isBlank()) {
                System.out.print("Deseja inserir observação? (s/n): ");
                String resp = sessao.getEntrada().nextLine().trim().toLowerCase();
                if (resp.startsWith("s")) {
                    while (true) {
                        System.out.printf("Informe a observação (máx. %d caracteres): ", LIMITE_CARACTERES_JUSTIFICATIVA);
                        String obs = sessao.getEntrada().nextLine().trim();
                        if (obs.length() > LIMITE_CARACTERES_JUSTIFICATIVA) {
                            System.out.printf("[AVISO] Observação excede o limite de %d caracteres (atual: %d). Digite novamente.%n",
                                    LIMITE_CARACTERES_JUSTIFICATIVA, obs.length());
                            continue;
                        }
                        if (!obs.isBlank()) {
                            motivo = obs;
                        }
                        break;
                    }
                }
            } else if (motivo.length() > LIMITE_CARACTERES_JUSTIFICATIVA) {
                motivo = motivo.substring(0, LIMITE_CARACTERES_JUSTIFICATIVA);
            }

            if (motivo != null && !motivo.isBlank()) {
                justFinal = String.format("Fechamento [%s: Esperado R$ %.2f, Contado R$ %.2f, Dif. %+,.2f] - Justificativa: \"%s\" - Operador: %s",
                        tipoDif, saldoEsperado, valorFisico, diferenca, motivo, sessao.getUsuarioLogado().getNomeUsuario());
            } else {
                justFinal = String.format("Fechamento [%s: Esperado R$ %.2f, Contado R$ %.2f, Dif. %+,.2f] pelo operador %s",
                        tipoDif, saldoEsperado, valorFisico, diferenca, sessao.getUsuarioLogado().getNomeUsuario());
            }
        } else {
            if (motivo == null || motivo.isBlank()) {
                System.out.print("Deseja inserir observação? (s/n): ");
                String resp = sessao.getEntrada().nextLine().trim().toLowerCase();
                if (resp.startsWith("s")) {
                    while (true) {
                        System.out.printf("Informe a observação (máx. %d caracteres): ", LIMITE_CARACTERES_JUSTIFICATIVA);
                        String obs = sessao.getEntrada().nextLine().trim();
                        if (obs.length() > LIMITE_CARACTERES_JUSTIFICATIVA) {
                            System.out.printf("[AVISO] Observação excede o limite de %d caracteres (atual: %d). Digite novamente.%n",
                                    LIMITE_CARACTERES_JUSTIFICATIVA, obs.length());
                            continue;
                        }
                        if (!obs.isBlank()) {
                            motivo = obs;
                        }
                        break;
                    }
                }
            } else if (motivo.length() > LIMITE_CARACTERES_JUSTIFICATIVA) {
                motivo = motivo.substring(0, LIMITE_CARACTERES_JUSTIFICATIVA);
            }

            if (motivo != null && !motivo.isBlank()) {
                justFinal = String.format("Fechamento [CAIXA EXATO: R$ %.2f] - %s", valorFisico, motivo);
            } else {
                justFinal = String.format("Fechamento [CAIXA EXATO: R$ %.2f] pelo operador %s", valorFisico, sessao.getUsuarioLogado().getNomeUsuario());
            }
        }

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarFechamentoCaixa(valorFisico, justFinal, idOperador);

        if (mov != null) {
            System.out.println();
            System.out.print(resumo.gerarRelatorioFechamento(valorFisico));
            if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
                String tipoDif = diferenca.compareTo(BigDecimal.ZERO) > 0 ? "SOBRA DE CAIXA" : "QUEBRA DE CAIXA";
                System.out.printf("\n[!] FECHAMENTO COM DIVERGÊNCIA REGISTRADA: %+,.2f (%s)%n", diferenca, tipoDif);
                if (motivo != null && !motivo.isBlank()) {
                    System.out.printf("JUSTIFICATIVA REGISTRADA: %s%n", motivo);
                }
            }
            System.out.println("\n[✓] CAIXA FÍSICO FECHADO COM SUCESSO!");
            System.out.println("O terminal está agora bloqueado para novas vendas até que uma nova abertura seja efetuada.");
        } else {
            System.out.println("[ERRO] Falha ao registrar fechamento do caixa no banco de dados.");
        }

        return true;
    }
}
