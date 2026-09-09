package projetopdv.ui.comandos.caixa;

import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;

public class ComandoHelpCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoHelpCaixa(SessaoCaixa sessao) {
        super("/help", "Exibe o manual de instruções e lista os comandos da Frente de Caixa.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        System.out.println("===============================================================================");
        System.out.println("                    MANUAL DE COMANDOS - FRENTE DE CAIXA (PDV)                 ");
        System.out.println("===============================================================================");
        if (sessao != null && sessao.estaAutenticado()) {
            System.out.printf("Operador: %s | Situação do Caixa: %s%n",
                    sessao.getUsuarioLogado().getNomeUsuario(),
                    sessao.getCaixaDAO().isCaixaAberto() ? "ABERTO" : "FECHADO");
        }
        System.out.println("\n[SISTEMA E AUTENTICAÇÃO]");
        System.out.println("  /login <usuario> <senha>   - Realiza login do operador de caixa.");
        System.out.println("  /logout                    - Encerra a sessão do operador atual.");
        System.out.println("  /help                      - Exibe esta ajuda.");
        System.out.println("  /sair                      - Finaliza o terminal do PDV.");

        System.out.println("\n[TURNO E GESTÃO DE GAVETA FÍSICA]");
        System.out.println("  /abrir_caixa [fundo]       - Realiza a abertura do caixa com fundo de troco inicial.");
        System.out.println("  /fechar_caixa              - Encerra o turno, apura os totais e faz conferência de caixa.");
        System.out.println("  /saldo                     - Consulta em tempo real a posição financeira da gaveta.");
        System.out.println("  /sangria <valor> [motivo]  - Registra retirada operacional de dinheiro para cofre.");
        System.out.println("  /suprimento <valor> [mot]  - Adiciona reforço de troco em dinheiro na gaveta.");

        System.out.println("\n[FATURAMENTO E VENDAS]");
        System.out.println("  /orcamentos                - Lista os orçamentos confirmados aguardando pagamento.");
        System.out.println("  /faturar [id_orcamento]    - Inicia o faturamento no caixa com cálculo de troco.");
        System.out.println("  /vendas                    - Lista as vendas realizadas no turno do caixa.");
        System.out.println("  /cupom <id_venda>          - Reimprime o cupom fiscal de uma venda.");

        System.out.println("\n[PÓS-VENDA, DEVOLUÇÕES E CRÉDITOS]");
        System.out.println("  /estornar <id_venda>       - Realiza estorno total (Reembolso Imediato ou Vale-Compra).");
        System.out.println("  /devolver <id_venda>       - Processa devolução parcial de item e gera Vale-Compra.");
        System.out.println("  /vales                     - Lista todos os vales-compra ativos.");
        System.out.println("  /resgatar_vale <codigo>    - Converte saldo de vale em dinheiro via sangria.");
        System.out.println("===============================================================================");
        return true;
    }
}
