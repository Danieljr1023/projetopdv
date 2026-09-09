package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoSuprimentoCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoSuprimentoCaixa(SessaoCaixa sessao) {
        super("/suprimento", "Realiza suprimento (entrada manual de troco ou reforço de dinheiro) na gaveta.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.VENDA_FATURAR)) {
            return true;
        }

        if (!sessao.getCaixaDAO().isCaixaAberto()) {
            System.out.println("[ERRO] O caixa precisa estar aberto para realizar suprimento. Utilize /abrir_caixa.");
            return true;
        }

        BigDecimal valor = null;
        String motivo = null;

        if (argumentos.length > 0) {
            try {
                valor = new BigDecimal(argumentos[0].replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor de suprimento inválido. Exemplo: /suprimento 50.00 Troco em moedas");
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
            System.out.print("Informe o valor do suprimento em dinheiro: R$ ");
            String inputValor = sessao.getEntrada().nextLine().trim().replace(",", ".");
            try {
                valor = new BigDecimal(inputValor);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido.");
                return true;
            }
        }

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("O valor do suprimento deve ser maior que zero.");
            return true;
        }

        if (motivo == null || motivo.isBlank()) {
            System.out.print("Informe o motivo/justificativa do suprimento (ex: Reforço de troco): ");
            motivo = sessao.getEntrada().nextLine().trim();
            if (motivo.isBlank()) {
                motivo = "Suprimento de troco";
            }
        }

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        try {
            MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarSuprimento(valor, motivo, idOperador);
            if (mov != null) {
                BigDecimal novoSaldo = sessao.getCaixaDAO().calcularSaldoDinheiroEmCaixa();
                System.out.println("\n====================================================");
                System.out.println("           COMPROVANTE DE SUPRIMENTO DE CAIXA       ");
                System.out.println("====================================================");
                System.out.printf("MOVIMENTAÇÃO #:        #%d%n", mov.getIdMovimentacao());
                System.out.printf("DATA / HORA:           %s%n", mov.getDataHora().format(SessaoCaixa.FORMATO_DATA_HORA));
                System.out.printf("OPERADOR:              %s%n", sessao.getUsuarioLogado().getNomeUsuario());
                System.out.printf("TIPO:                  %s%n", mov.getTipoMovimentacao().getDescricao());
                System.out.printf("VALOR ENTRADA:         R$ %.2f%n", valor);
                System.out.printf("JUSTIFICATIVA:         %s%n", motivo);
                System.out.println("----------------------------------------------------");
                System.out.printf("NOVO SALDO GAVETA:     R$ %.2f%n", novoSaldo);
                System.out.println("====================================================");
            } else {
                System.out.println("[ERRO] Não foi possível registrar o suprimento no banco de dados.");
            }
        } catch (Exception e) {
            System.out.println("[ERRO] " + e.getMessage());
        }

        return true;
    }
}
