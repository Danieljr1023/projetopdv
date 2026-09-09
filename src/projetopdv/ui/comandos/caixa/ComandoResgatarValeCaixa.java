package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import java.util.List;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.posvenda.ValeCompra;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoResgatarValeCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoResgatarValeCaixa(SessaoCaixa sessao) {
        super(
                "/resgatar_vale",
                """
                Resgata o saldo de um vale-compra em dinheiro físico via Sangria de Caixa.

                Usos:
                /resgatar_vale          (Modo interativo: lista vales ativos para seleção rápida)
                /resgatar_vale <CÓDIGO> (Resgate direto)
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.CAIXA_SANGRIA)) {
            return true;
        }
        if (!sessao.validarCaixaAberto()) {
            return true;
        }

        String codigo;
        if (argumentos.length > 0) {
            codigo = argumentos[0].trim().toUpperCase();
        } else {
            List<ValeCompra> valesAtivos = sessao.getPosVendaDAO().listarValesAtivos();
            if (valesAtivos.isEmpty()) {
                System.out.println("Nenhum vale-compra ativo encontrado no momento.");
                System.out.print("Informe o código do Vale-Compra a ser resgatado (ou 'cancelar'): ");
                codigo = sessao.getEntrada().nextLine().trim().toUpperCase();
            } else {
                System.out.println("\n==========================================================================================");
                System.out.printf("                        VALES-COMPRA ATIVOS (%d encontrados)                              %n", valesAtivos.size());
                System.out.println("==========================================================================================");
                System.out.printf("%-5s | %-20s | %-12s | %-12s | %-15s%n", "OPÇÃO", "CÓDIGO", "ORIGINAL", "SALDO DISP.", "SITUAÇÃO");
                System.out.println("------------------------------------------------------------------------------------------");
                for (int i = 0; i < valesAtivos.size(); i++) {
                    ValeCompra v = valesAtivos.get(i);
                    System.out.printf("[%2d]  | %-20s | R$ %9.2f | R$ %9.2f | %-15s%n",
                            (i + 1), v.getCodigoVale(), v.getValorOriginal(), v.getSaldo(), v.getStatusVale().getDescricao());
                }
                System.out.println("==========================================================================================");
                System.out.print("Escolha o número da opção desejada ou informe o código (ou 'cancelar'): ");
                String input = sessao.getEntrada().nextLine().trim();
                if (input.isEmpty() || input.equalsIgnoreCase("cancelar")) {
                    System.out.println("Operação cancelada.");
                    return true;
                }
                try {
                    int sel = Integer.parseInt(input);
                    if (sel >= 1 && sel <= valesAtivos.size()) {
                        codigo = valesAtivos.get(sel - 1).getCodigoVale();
                    } else {
                        codigo = input.toUpperCase();
                    }
                } catch (NumberFormatException e) {
                    codigo = input.toUpperCase();
                }
            }
        }

        if (codigo.isEmpty()) {
            System.out.println("Código do vale não informado.");
            return true;
        }

        ValeCompra vale = sessao.getPosVendaDAO().buscarValePorCodigo(codigo);
        if (vale == null) {
            System.out.printf("[ERRO] Vale-compra '%s' não encontrado.%n", codigo);
            return true;
        }

        if (!vale.podeSerUsado()) {
            System.out.printf("[ERRO] O vale-compra '%s' não está ativo para resgate (Situação: %s).%n",
                    codigo, vale.getStatusVale().getDescricao());
            return true;
        }

        System.out.println("\n====================================================");
        System.out.println("           RESGATE DE VALE-COMPRA EM DINHEIRO       ");
        System.out.println("====================================================");
        System.out.printf("CÓDIGO:           %s%n", vale.getCodigoVale());
        System.out.printf("SALDO DISPONÍVEL: R$ %.2f%n", vale.getSaldo());
        System.out.println("----------------------------------------------------");

        System.out.printf("Informe o valor a resgatar em dinheiro (Pressione ENTER para resgatar o total de R$ %.2f): R$ ", vale.getSaldo());
        String inputValor = sessao.getEntrada().nextLine().trim().replace(",", ".");
        BigDecimal valorResgate;
        if (inputValor.isEmpty()) {
            valorResgate = vale.getSaldo();
        } else {
            try {
                valorResgate = new BigDecimal(inputValor);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido.");
                return true;
            }
        }

        if (valorResgate.compareTo(BigDecimal.ZERO) <= 0 || valorResgate.compareTo(vale.getSaldo()) > 0) {
            System.out.printf("[ERRO] Valor a resgatar deve ser entre R$ 0,01 e R$ %.2f.%n", vale.getSaldo());
            return true;
        }

        // Verificar se há dinheiro físico suficiente na gaveta
        BigDecimal saldoGaveta = sessao.getCaixaDAO().calcularSaldoDinheiroEmCaixa();
        if (saldoGaveta.compareTo(valorResgate) < 0) {
            System.out.printf("[ALERTA DE CAIXA] O saldo em dinheiro na gaveta (R$ %.2f) é inferior ao valor do resgate solicitado (R$ %.2f).%n",
                    saldoGaveta, valorResgate);
            System.out.print("Deseja prosseguir mesmo assim? (S/N): ");
            String conf = sessao.getEntrada().nextLine().trim().toUpperCase();
            if (!conf.startsWith("S")) {
                System.out.println("Resgate cancelado.");
                return true;
            }
        }

        System.out.print("Informe a justificativa / observação do resgate: ");
        String just = sessao.getEntrada().nextLine().trim();

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        MovimentacaoCaixa mov = sessao.getCaixaDAO().registrarSangriaResgateVale(
                vale.getIdValeCompra(),
                valorResgate,
                just.isEmpty() ? "Resgate de vale " + codigo : just,
                idOperador
        );

        if (mov != null) {
            System.out.println("\n====================================================");
            System.out.println("          SANGRIA DE RESGATE REGISTRADA!            ");
            System.out.println("====================================================");
            System.out.printf("VALOR ENTREGUE AO CLIENTE: R$ %.2f EM ESPÉCIE%n", valorResgate);
            System.out.printf("REGISTRO DE SAÍDA:        #%d%n", mov.getIdMovimentacao());
            System.out.printf("DATA / HORA:              %s%n", mov.getDataHora().format(SessaoCaixa.FORMATO_DATA_HORA));
            System.out.println("====================================================");
        } else {
            System.out.println("[ERRO] Falha ao registrar a sangria de resgate de vale no caixa.");
        }

        return true;
    }
}
