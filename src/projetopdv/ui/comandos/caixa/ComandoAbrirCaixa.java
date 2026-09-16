package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoAbrirCaixa extends ComandoPainel {

    private static final int LIMITE_CARACTERES_JUSTIFICATIVA = 200;

    private final SessaoCaixa sessao;

    public ComandoAbrirCaixa(SessaoCaixa sessao) {
        super("/abrir_caixa", "Abre o caixa físico para início de turno com fundo de troco inicial.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.VENDA_FATURAR)) {
            return true;
        }

        if (sessao.getCaixaDAO().isCaixaAberto()) {
            System.out.println("[AVISO] O caixa já se encontra aberto!");
            System.out.printf("Saldo atual da gaveta: R$ %.2f%n", sessao.getCaixaDAO().calcularSaldoDinheiroEmCaixa());
            return true;
        }

        MovimentacaoCaixa ultimoFechamento = sessao.getCaixaDAO().obterUltimoFechamento();
        BigDecimal saldoAnterior = ultimoFechamento != null ? ultimoFechamento.getValor() : null;

        BigDecimal fundoTroco = null;
        if (argumentos.length > 0) {
            try {
                fundoTroco = new BigDecimal(argumentos[0].replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor de fundo de troco inválido. Exemplo: /abrir_caixa 100.00");
                return true;
            }
        }

        // Loop de conferência e recontagem
        while (true) {
            if (fundoTroco == null) {
                if (saldoAnterior != null) {
                    System.out.printf("Saldo em gaveta registrado no último fechamento: R$ %.2f%n", saldoAnterior);
                }
                System.out.print("Informe o valor do fundo de troco inicial em dinheiro: R$ ");
                String input = sessao.getEntrada().nextLine().trim().replace(",", ".");
                if (input.isEmpty()) {
                    fundoTroco = BigDecimal.ZERO;
                } else {
                    try {
                        fundoTroco = new BigDecimal(input);
                    } catch (NumberFormatException e) {
                        System.out.println("Valor inválido.");
                        fundoTroco = null;
                        continue;
                    }
                }
            }

            if (fundoTroco.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("O fundo de troco não pode ser negativo.");
                fundoTroco = null;
                continue;
            }

            // Se existe fechamento anterior, verificar se há divergência
            if (saldoAnterior != null && fundoTroco.compareTo(saldoAnterior) != 0) {
                BigDecimal dif = fundoTroco.subtract(saldoAnterior);
                String tipoDif = dif.compareTo(BigDecimal.ZERO) > 0 ? "SOBRA NA ABERTURA" : "FALTA NA ABERTURA";

                System.out.println("\n========================================================================");
                System.out.println("      [!] ATENÇÃO: DIVERGÊNCIA IDENTIFICADA NA ABERTURA DA GAVETA!      ");
                System.out.println("========================================================================");
                System.out.printf("SALDO DEIXADO NO ÚLTIMO FECHAMENTO: R$ %10.2f%n", saldoAnterior);
                System.out.printf("VALOR INFORMADO NA CONTAGEM ATUAL:  R$ %10.2f%n", fundoTroco);
                System.out.printf("DIFERENÇA CONSTATADA:               %+10.2f (%s)%n", dif, tipoDif);
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Opções disponíveis:");
                System.out.println("  [C] Confirmar abertura com esta divergência (ficará auditado no caixa)");
                System.out.println("  [R] Recontar o dinheiro da gaveta e informar novo valor");
                System.out.println("  [X] Cancelar abertura");
                System.out.print("Escolha a opção (C/R/X): ");

                String opcao = sessao.getEntrada().nextLine().trim().toUpperCase();
                if (opcao.startsWith("R")) {
                    System.out.println("\n--- RECONTAGEM DA GAVETA ---");
                    fundoTroco = null;
                    continue;
                } else if (opcao.startsWith("X")) {
                    System.out.println("Abertura de caixa cancelada pelo operador.");
                    return true;
                } else if (!opcao.startsWith("C")) {
                    System.out.println("Opção inválida. Operação abortada por segurança.");
                    return true;
                }
            }

            break;
        }

        if (fundoTroco == null) {
            return true;
        }

        String justFinal;
        String justificativaDivergencia = null;
        if (saldoAnterior != null && fundoTroco.compareTo(saldoAnterior) != 0) {
            BigDecimal dif = fundoTroco.subtract(saldoAnterior);
            String tipoDif = dif.compareTo(BigDecimal.ZERO) > 0 ? "SOBRA NA ABERTURA" : "FALTA NA ABERTURA";

            if (argumentos.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < argumentos.length; i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(argumentos[i]);
                }
                justificativaDivergencia = sb.toString().trim();
            }

            if (justificativaDivergencia == null || justificativaDivergencia.isBlank()) {
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
                            justificativaDivergencia = obs;
                        }
                        break;
                    }
                }
            } else if (justificativaDivergencia.length() > LIMITE_CARACTERES_JUSTIFICATIVA) {
                justificativaDivergencia = justificativaDivergencia.substring(0, LIMITE_CARACTERES_JUSTIFICATIVA);
            }

            if (justificativaDivergencia != null && !justificativaDivergencia.isBlank()) {
                justFinal = String.format(
                        "Abertura de turno [DIVERGÊNCIA CONFIRMADA: Último fechamento R$ %.2f, Contado R$ %.2f, Dif. %+,.2f (%s)] - Justificativa: \"%s\" - Operador: %s",
                        saldoAnterior, fundoTroco, dif, tipoDif, justificativaDivergencia, sessao.getUsuarioLogado().getNomeUsuario()
                );
            } else {
                justFinal = String.format(
                        "Abertura de turno [DIVERGÊNCIA CONFIRMADA: Último fechamento R$ %.2f, Contado R$ %.2f, Dif. %+,.2f (%s)] pelo operador %s",
                        saldoAnterior, fundoTroco, dif, tipoDif, sessao.getUsuarioLogado().getNomeUsuario()
                );
            }
        } else if (saldoAnterior != null) {
            justFinal = String.format(
                    "Abertura de turno pelo operador %s (Fundo de troco conferido com último fechamento: R$ %.2f)",
                    sessao.getUsuarioLogado().getNomeUsuario(), fundoTroco
            );
        } else {
            justFinal = "Abertura de turno inicial pelo operador " + sessao.getUsuarioLogado().getNomeUsuario();
        }

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarAberturaCaixa(
                fundoTroco,
                justFinal,
                idOperador
        );

        if (mov != null) {
            System.out.println("\n====================================================");
            System.out.println("           CAIXA FÍSICO ABERTO COM SUCESSO!         ");
            System.out.println("====================================================");
            System.out.printf("OPERADOR RESPONSÁVEL:   %s%n", sessao.getUsuarioLogado().getNomeUsuario());
            System.out.printf("DATA / HORA DE ABERTURA: %s%n", mov.getDataHora().format(SessaoCaixa.FORMATO_DATA_HORA));
            System.out.printf("FUNDO DE TROCO INICIAL:  R$ %.2f%n", fundoTroco);
            if (saldoAnterior != null && fundoTroco.compareTo(saldoAnterior) != 0) {
                BigDecimal dif = fundoTroco.subtract(saldoAnterior);
                String tipoDif = dif.compareTo(BigDecimal.ZERO) > 0 ? "SOBRA NA ABERTURA" : "FALTA NA ABERTURA";
                System.out.printf("STATUS CONFERÊNCIA:     [!] DIVERGÊNCIA REGISTRADA (%+,.2f - %s)%n", dif, tipoDif);
                if (justificativaDivergencia != null && !justificativaDivergencia.isBlank()) {
                    System.out.printf("JUSTIFICATIVA:          %s%n", justificativaDivergencia);
                }
            } else if (saldoAnterior != null) {
                System.out.println("STATUS CONFERÊNCIA:     [✓] CONFERIDO COM ÚLTIMO FECHAMENTO");
            }
            System.out.println("O terminal está pronto para faturamento de vendas!");
            System.out.println("====================================================");
        } else {
            System.out.println("[ERRO] Falha ao registrar abertura de caixa no banco de dados.");
        }

        return true;
    }
}
