package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoAbrirCaixa extends ComandoPainel {

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

        BigDecimal fundoTroco;
        if (argumentos.length > 0) {
            try {
                fundoTroco = new BigDecimal(argumentos[0].replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor de fundo de troco inválido. Exemplo: /abrir_caixa 100.00");
                return true;
            }
        } else {
            System.out.print("Informe o valor do fundo de troco inicial em dinheiro: R$ ");
            String input = sessao.getEntrada().nextLine().trim().replace(",", ".");
            if (input.isEmpty()) {
                fundoTroco = BigDecimal.ZERO;
            } else {
                try {
                    fundoTroco = new BigDecimal(input);
                } catch (NumberFormatException e) {
                    System.out.println("Valor inválido.");
                    return true;
                }
            }
        }

        if (fundoTroco.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("O fundo de troco não pode ser negativo.");
            return true;
        }

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarAberturaCaixa(
                fundoTroco,
                "Abertura de turno pelo operador " + sessao.getUsuarioLogado().getNomeUsuario(),
                idOperador
        );

        if (mov != null) {
            System.out.println("\n====================================================");
            System.out.println("           CAIXA FÍSICO ABERTO COM SUCESSO!         ");
            System.out.println("====================================================");
            System.out.printf("OPERADOR RESPONSÁVEL:   %s%n", sessao.getUsuarioLogado().getNomeUsuario());
            System.out.printf("DATA / HORA DE ABERTURA: %s%n", mov.getDataHora().format(SessaoCaixa.FORMATO_DATA_HORA));
            System.out.printf("FUNDO DE TROCO INICIAL:  R$ %.2f%n", fundoTroco);
            System.out.println("O terminal está pronto para faturamento de vendas!");
            System.out.println("====================================================");
        } else {
            System.out.println("[ERRO] Falha ao registrar abertura de caixa no banco de dados.");
        }

        return true;
    }
}
