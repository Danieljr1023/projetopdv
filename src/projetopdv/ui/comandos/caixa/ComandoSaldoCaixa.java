package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import projetopdv.caixa.ResumoTurnoCaixa;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoSaldoCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoSaldoCaixa(SessaoCaixa sessao) {
        super("/saldo", "Exibe o saldo em espécie atual na gaveta do caixa e resumo de faturamento do turno.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.VENDA_FATURAR)) {
            return true;
        }

        boolean aberto = sessao.getCaixaDAO().isCaixaAberto();
        BigDecimal saldoGaveta = sessao.getCaixaDAO().calcularSaldoDinheiroEmCaixa();
        ResumoTurnoCaixa resumo = sessao.getCaixaDAO().obterResumoTurnoAtual();

        System.out.println("\n====================================================");
        System.out.println("                 SITUAÇÃO DO CAIXA FÍSICO           ");
        System.out.println("====================================================");
        System.out.printf("STATUS DA GAVETA:          %s%n", aberto ? "[ABERTO / EM OPERAÇÃO]" : "[FECHADO]");
        if (resumo.getDataAbertura() != null) {
            System.out.printf("DATA/HORA DE ABERTURA:     %s%n", resumo.getDataAbertura().format(SessaoCaixa.FORMATO_DATA_HORA));
            System.out.printf("FUNDO DE TROCO INICIAL:    R$ %15.2f%n", resumo.getFundoTrocoInicial());
        }
        System.out.println("----------------------------------------------------");
        System.out.printf("SALDO ATUAL EM ESPÉCIE:    R$ %15.2f%n", saldoGaveta);
        System.out.println("----------------------------------------------------");
        System.out.println("FATURAMENTO DO TURNO:");
        System.out.printf("  (+) DINHEIRO:            R$ %15.2f%n", resumo.getTotalVendasDinheiro());
        System.out.printf("  (+) CARTÃO DE CRÉDITO:   R$ %15.2f%n", resumo.getTotalVendasCartaoCredito());
        System.out.printf("  (+) CARTÃO DE DÉBITO:    R$ %15.2f%n", resumo.getTotalVendasCartaoDebito());
        System.out.printf("  (+) PIX:                 R$ %15.2f%n", resumo.getTotalVendasPix());
        System.out.printf("  (+) VALE-COMPRA:         R$ %15.2f%n", resumo.getTotalVendasValeCompra());
        System.out.println("----------------------------------------------------");
        System.out.printf("TOTAL FATURADO NO TURNO:   R$ %15.2f%n", resumo.getTotalFaturado());
        System.out.printf("TOTAL DE VENDAS:           %d vendas (%d itens)%n", resumo.getTotalVendasRealizadas(), resumo.getTotalItensVendidos());
        System.out.println("====================================================");

        return true;
    }
}
