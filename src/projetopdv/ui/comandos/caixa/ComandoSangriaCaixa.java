package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoSangriaCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoSangriaCaixa(SessaoCaixa sessao) {
        super("/sangria", "Realiza sangria (retirada manual de dinheiro em espécie) da gaveta.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.CAIXA_SANGRIA)) {
            return true;
        }

        if (!sessao.getCaixaDAO().isCaixaAberto()) {
            System.out.println("[ERRO] O caixa precisa estar aberto para realizar sangria. Utilize /abrir_caixa.");
            return true;
        }

        BigDecimal saldoAtual = sessao.getCaixaDAO().calcularSaldoDinheiroEmCaixa();
        System.out.printf("Saldo disponível na gaveta: R$ %.2f%n", saldoAtual);

        if (saldoAtual.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[ERRO] Não há saldo em dinheiro disponível na gaveta para retirada.");
            return true;
        }

        BigDecimal valor = null;
        String motivo = null;

        if (argumentos.length > 0) {
            try {
                valor = new BigDecimal(argumentos[0].replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor de sangria inválido. Exemplo: /sangria 150.00 Cofre principal");
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

        if (valor == null) {
            System.out.print("Informe o valor da sangria em dinheiro: R$ ");
            String inputValor = sessao.getEntrada().nextLine().trim().replace(",", ".");
            try {
                valor = new BigDecimal(inputValor);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido.");
                return true;
            }
        }

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("O valor da sangria deve ser maior que zero.");
            return true;
        }

        if (valor.compareTo(saldoAtual) > 0) {
            System.out.printf("[ERRO] Valor solicitado (R$ %.2f) é superior ao saldo da gaveta (R$ %.2f).%n", valor, saldoAtual);
            return true;
        }

        if (motivo == null || motivo.isBlank()) {
            System.out.print("Informe o motivo/justificativa da sangria (ex: Transferência para cofre): ");
            motivo = sessao.getEntrada().nextLine().trim();
            if (motivo.isBlank()) {
                motivo = "Sangria operacional de rotina";
            }
        }

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        try {
            MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarSangriaOperacional(valor, motivo, idOperador);
            if (mov != null) {
                BigDecimal novoSaldo = sessao.getCaixaDAO().calcularSaldoDinheiroEmCaixa();
                System.out.println("\n====================================================");
                System.out.println("            COMPROVANTE DE SANGRIA DE CAIXA         ");
                System.out.println("====================================================");
                System.out.printf("MOVIMENTAÇÃO #:        #%d%n", mov.getIdMovimentacao());
                System.out.printf("DATA / HORA:           %s%n", mov.getDataHora().format(SessaoCaixa.FORMATO_DATA_HORA));
                System.out.printf("OPERADOR:              %s%n", sessao.getUsuarioLogado().getNomeUsuario());
                System.out.printf("TIPO:                  %s%n", mov.getTipoMovimentacao().getDescricao());
                System.out.printf("VALOR RETIRADO:        R$ %.2f%n", valor);
                System.out.printf("JUSTIFICATIVA:         %s%n", motivo);
                System.out.println("----------------------------------------------------");
                System.out.printf("SALDO RESTANTE GAVETA: R$ %.2f%n", novoSaldo);
                System.out.println("====================================================");
            } else {
                System.out.println("[ERRO] Não foi possível registrar a sangria no banco de dados.");
            }
        } catch (Exception e) {
            System.out.println("[ERRO] " + e.getMessage());
        }

        return true;
    }
}
