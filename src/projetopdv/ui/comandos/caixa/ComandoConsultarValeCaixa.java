package projetopdv.ui.comandos.caixa;

import java.util.List;
import projetopdv.posvenda.ValeCompra;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;

public class ComandoConsultarValeCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoConsultarValeCaixa(SessaoCaixa sessao) {
        super("/vales", "Consulta vales-compra ativos ou detalha um vale específico por código.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarAutenticado()) {
            return true;
        }

        if (argumentos.length > 0) {
            String codigo = argumentos[0].trim().toUpperCase();
            ValeCompra vale = sessao.getPosVendaDAO().buscarValePorCodigo(codigo);
            if (vale == null) {
                System.out.printf("[ERRO] Vale-compra '%s' não encontrado.%n", codigo);
                return true;
            }
            System.out.println("\n" + vale.gerarComprovante());
            return true;
        }

        List<ValeCompra> vales = sessao.getPosVendaDAO().listarValesAtivos();
        if (vales.isEmpty()) {
            System.out.println("Nenhum vale-compra ativo no momento.");
            return true;
        }

        System.out.println("\n==========================================================================================");
        System.out.printf("                        VALES-COMPRA ATIVOS (%d encontrados)                              %n", vales.size());
        System.out.println("==========================================================================================");
        System.out.printf("%-20s | %-12s | %-12s | %-19s | %-15s%n", "CÓDIGO", "ORIGINAL", "SALDO DISP.", "EMISSÃO", "SITUAÇÃO");
        System.out.println("------------------------------------------------------------------------------------------");

        for (ValeCompra v : vales) {
            System.out.printf("%-20s | R$ %9.2f | R$ %9.2f | %-19s | %-15s%n",
                    v.getCodigoVale(),
                    v.getValorOriginal(),
                    v.getSaldo(),
                    v.getDataEmissao().format(SessaoCaixa.FORMATO_DATA_HORA),
                    v.getStatusVale().getDescricao()
            );
        }

        System.out.println("==========================================================================================");
        System.out.println("DICA: Para ver detalhes de um vale, digite: /vales <CODIGO>");
        System.out.println("DICA: Para resgatar um vale em dinheiro na gaveta, digite: /resgatar_vale <CODIGO>");
        return true;
    }
}
