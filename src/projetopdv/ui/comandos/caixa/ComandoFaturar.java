package projetopdv.ui.comandos.caixa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.posvenda.ValeCompra;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;
import projetopdv.venda.FormaPagamento;
import projetopdv.venda.Venda;

public class ComandoFaturar extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoFaturar(SessaoCaixa sessao) {
        super(
                "/faturar",
                """
                Inicia o recebimento e faturamento no caixa de um orçamento confirmado.

                Usos:
                /faturar               (Modo interativo: lista orçamentos confirmados para seleção rápida)
                /faturar <ID Orçamento> (Faturamento direto)
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.VENDA_FATURAR)) {
            return true;
        }
        if (!sessao.validarCaixaAberto()) {
            return true;
        }

        int idOrcamento;
        if (argumentos.length > 0) {
            try {
                idOrcamento = Integer.parseInt(argumentos[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("ID do orçamento inválido. Exemplo: /faturar 12");
                return true;
            }
        } else {
            // Modo interativo: listar orçamentos CONFIRMADOS aguardando faturamento
            List<Orcamento> confirmados;
            try {
                confirmados = sessao.getOrcamentoDAO().listarPorStatus(StatusOrcamento.CONFIRMADO);
            } catch (java.sql.SQLException e) {
                System.out.println("[ERRO] Erro ao consultar orçamentos: " + e.getMessage());
                return true;
            }

            if (confirmados.isEmpty()) {
                System.out.println("\n[AVISO] Nenhum orçamento confirmado aguardando faturamento no momento.");
                System.out.print("Se desejar informar um ID manualmente, digite o ID (ou 'cancelar'): ");
                String input = sessao.getEntrada().nextLine().trim();
                if (input.isEmpty() || input.equalsIgnoreCase("cancelar")) {
                    return true;
                }
                try {
                    idOrcamento = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("ID inválido.");
                    return true;
                }
            } else {
                System.out.println("\n==========================================================================================");
                System.out.printf("               ORÇAMENTOS CONFIRMADOS AGUARDANDO FATURAMENTO (%d encontrados)              %n", confirmados.size());
                System.out.println("==========================================================================================");
                System.out.printf("%-5s | %-6s | %-25s | %-25s | %-12s%n", "OPÇÃO", "ID", "NOME DO ORÇAMENTO", "CLIENTE", "VALOR TOTAL");
                System.out.println("------------------------------------------------------------------------------------------");
                for (int i = 0; i < confirmados.size(); i++) {
                    Orcamento o = confirmados.get(i);
                    String nomeCli = o.temCliente() ? o.getCliente().getNomeCliente() : "Consumidor Final";
                    if (nomeCli.length() > 25) nomeCli = nomeCli.substring(0, 23) + "..";
                    String nomeOrc = o.getNomeOrcamento();
                    if (nomeOrc.length() > 25) nomeOrc = nomeOrc.substring(0, 23) + "..";
                    System.out.printf("[%2d]  | #%-5d | %-25s | %-25s | R$ %10.2f%n",
                            (i + 1), o.getIdOrcamento(), nomeOrc, nomeCli, o.getValorTotal());
                }
                System.out.println("==========================================================================================");
                System.out.print("Escolha o número da opção desejada ou informe o ID (ou 'cancelar'): ");
                String input = sessao.getEntrada().nextLine().trim();
                if (input.isEmpty() || input.equalsIgnoreCase("cancelar")) {
                    System.out.println("Faturamento cancelado.");
                    return true;
                }
                try {
                    int sel = Integer.parseInt(input);
                    if (sel >= 1 && sel <= confirmados.size()) {
                        idOrcamento = confirmados.get(sel - 1).getIdOrcamento();
                    } else {
                        idOrcamento = sel;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Opção inválida.");
                    return true;
                }
            }
        }

        Orcamento orcamento = sessao.getOrcamentoDAO().buscarPorId(idOrcamento);
        if (orcamento == null) {
            System.out.printf("[ERRO] Orçamento #%d não encontrado.%n", idOrcamento);
            return true;
        }

        if (orcamento.getStatusOrcamento() == StatusOrcamento.FINALIZADO) {
            System.out.printf("[AVISO] O orçamento #%d já foi finalizado e faturado anteriormente!%n", idOrcamento);
            if (orcamento.temVenda()) {
                System.out.printf("Venda correspondente: #%d (Total: R$ %.2f)%n",
                        orcamento.getVenda().getIdVenda(), orcamento.getVenda().getValorTotal());
                System.out.printf("Para consultar o cupom, digite: /cupom %d%n", orcamento.getVenda().getIdVenda());
            }
            return true;
        }

        if (orcamento.getStatusOrcamento() != StatusOrcamento.CONFIRMADO && orcamento.getStatusOrcamento() != StatusOrcamento.FATURANDO) {
            System.out.printf("[ERRO] O orçamento #%d está no status '%s' e não pode ser faturado.%n",
                    idOrcamento, orcamento.getStatusOrcamento());
            System.out.println("Apenas orçamentos no status 'CONFIRMADO' podem ser recebidos no caixa.");
            return true;
        }

        if (orcamento.getItensOrcamento().isEmpty()) {
            System.out.println("[ERRO] Este orçamento não possui itens.");
            return true;
        }

        // Exibir Resumo do Orçamento
        BigDecimal subtotalBruto = orcamento.getItensOrcamento().stream()
                .map(ItemOrcamento::getValorSubtotalBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descontoTotal = orcamento.getItensOrcamento().stream()
                .map(ItemOrcamento::getValorDescontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAPagar = orcamento.getValorTotal();

        System.out.println("\n====================================================");
        System.out.printf("        FATURAMENTO DO ORÇAMENTO #%06d             %n", idOrcamento);
        System.out.println("====================================================");
        System.out.printf("NOME:           %s%n", orcamento.getNomeOrcamento());
        System.out.printf("CLIENTE:        %s%n", orcamento.temCliente() ? orcamento.getCliente().getNomeCliente() : "Consumidor Final");
        System.out.printf("TOTAL DE ITENS: %d un (%d produtos distintos)%n",
                orcamento.getItensOrcamento().stream().mapToInt(ItemOrcamento::getQuantidade).sum(),
                orcamento.getItensOrcamento().size());
        System.out.println("----------------------------------------------------");
        System.out.printf("SUBTOTAL BRUTO:                 R$ %15.2f%n", subtotalBruto);
        if (descontoTotal.compareTo(BigDecimal.ZERO) > 0) {
            System.out.printf("DESCONTOS APLICADOS:          (-) R$ %15.2f%n", descontoTotal);
        }
        System.out.printf("VALOR TOTAL A PAGAR:            R$ %15.2f%n", totalAPagar);
        System.out.println("====================================================");

        FormaPagamento forma;
        String codigoVale = null;

        if (argumentos.length > 1) {
            String formaArg = argumentos[1].trim().toUpperCase();
            switch (formaArg) {
                case "1", "DINHEIRO" -> forma = FormaPagamento.DINHEIRO;
                case "2", "PIX" -> forma = FormaPagamento.PIX;
                case "3", "CARTAO_CREDITO", "CREDITO" -> forma = FormaPagamento.CARTAO_CREDITO;
                case "4", "CARTAO_DEBITO", "DEBITO" -> forma = FormaPagamento.CARTAO_DEBITO;
                case "5", "VALE_COMPRA", "VALE" -> forma = FormaPagamento.VALE_COMPRA;
                default -> {
                    System.out.printf("[ERRO] Forma de pagamento '%s' não reconhecida.%n", formaArg);
                    return true;
                }
            }
        } else {
            // Seleção da Forma de Pagamento
            System.out.println("FORMAS DE PAGAMENTO:");
            System.out.println("  [1] Dinheiro");
            System.out.println("  [2] PIX");
            System.out.println("  [3] Cartão de Crédito");
            System.out.println("  [4] Cartão de Débito");
            System.out.println("  [5] Vale-Compra");
            System.out.println("  [0] Cancelar Operação");
            System.out.print("Selecione a opção desejada: ");

            String opcao = sessao.getEntrada().nextLine().trim();
            switch (opcao) {
                case "1" -> forma = FormaPagamento.DINHEIRO;
                case "2" -> forma = FormaPagamento.PIX;
                case "3" -> forma = FormaPagamento.CARTAO_CREDITO;
                case "4" -> forma = FormaPagamento.CARTAO_DEBITO;
                case "5" -> forma = FormaPagamento.VALE_COMPRA;
                case "0" -> {
                    System.out.println("Operação de faturamento cancelada pelo operador.");
                    return true;
                }
                default -> {
                    System.out.println("Opção inválida. Operação abortada.");
                    return true;
                }
            }
        }

        // Fluxo específico de pagamento em Dinheiro (Cálculo de Troco)
        if (forma == FormaPagamento.DINHEIRO) {
            System.out.printf("Valor total a pagar: R$ %.2f%n", totalAPagar);
            BigDecimal valorRecebido;
            if (argumentos.length > 2) {
                try {
                    valorRecebido = new BigDecimal(argumentos[2].replace(",", "."));
                } catch (NumberFormatException e) {
                    System.out.println("Valor recebido inválido informado nos argumentos.");
                    return true;
                }
            } else {
                System.out.print("Informe o valor recebido em dinheiro do cliente: R$ ");
                String inputValor = sessao.getEntrada().nextLine().trim().replace(",", ".");
                try {
                    valorRecebido = new BigDecimal(inputValor);
                } catch (NumberFormatException e) {
                    System.out.println("Valor recebido inválido. Faturamento cancelado.");
                    return true;
                }
            }

            if (valorRecebido.compareTo(totalAPagar) < 0) {
                BigDecimal falta = totalAPagar.subtract(valorRecebido);
                System.out.printf("[PAGAMENTO INSUFICIENTE] Valor recebido (R$ %.2f) é menor que o total da venda. Faltam R$ %.2f.%n",
                        valorRecebido, falta);
                return true;
            }

            BigDecimal troco = valorRecebido.subtract(totalAPagar).setScale(2, RoundingMode.HALF_UP);
            System.out.println("----------------------------------------------------");
            System.out.printf("VALOR RECEBIDO:   R$ %10.2f%n", valorRecebido);
            System.out.printf("TOTAL DA VENDA:   R$ %10.2f%n", totalAPagar);
            System.out.printf("TROCO A DEVOLVER: R$ %10.2f%n", troco);
            System.out.println("----------------------------------------------------");
        }

        // Fluxo específico de Vale-Compra
        if (forma == FormaPagamento.VALE_COMPRA) {
            if (argumentos.length > 2) {
                codigoVale = argumentos[2].trim().toUpperCase();
            } else {
                System.out.print("Informe o código do Vale-Compra (ex.: VALE-YYYYMMDD-XXXX): ");
                codigoVale = sessao.getEntrada().nextLine().trim().toUpperCase();
            }

            ValeCompra vale = sessao.getPosVendaDAO().buscarValePorCodigo(codigoVale);
            if (vale == null) {
                System.out.printf("[ERRO] Vale-compra '%s' não encontrado.%n", codigoVale);
                return true;
            }
            if (!vale.podeSerUsado()) {
                System.out.printf("[ERRO] Vale-compra '%s' não está ativo para uso (Situação: %s).%n",
                        codigoVale, vale.getStatusVale().getDescricao());
                return true;
            }
            if (vale.getSaldo().compareTo(totalAPagar) < 0) {
                System.out.printf("[SALDO INSUFICIENTE] O vale possui saldo de R$ %.2f, mas a compra é de R$ %.2f.%n",
                        vale.getSaldo(), totalAPagar);
                System.out.println("Faturamento abortado.");
                return true;
            }

            System.out.printf("✓ Vale-compra validado! Saldo atual: R$ %.2f | Novo saldo após compra: R$ %.2f%n",
                    vale.getSaldo(), vale.getSaldo().subtract(totalAPagar));
        }

        // Confirmação final
        boolean autoConfirmado = argumentos.length >= 3 || (argumentos.length >= 2 && forma != FormaPagamento.DINHEIRO && forma != FormaPagamento.VALE_COMPRA);
        if (!autoConfirmado) {
            System.out.print("\nConfirmar conclusão do pagamento e emissão da venda? (S/N): ");
            String confirma = sessao.getEntrada().nextLine().trim().toUpperCase();
            if (!confirma.startsWith("S")) {
                System.out.println("Faturamento cancelado pelo operador.");
                return true;
            }
        }

        // Execução Transacional no VendaDAO
        if (forma == null) {
            System.out.println("[ERRO] Forma de pagamento não definida.");
            return true;
        }

        Venda vendaGerada = sessao.getVendaDAO().faturarOrcamento(orcamento, forma.getDescricao(), codigoVale);
        if (vendaGerada != null) {
            System.out.println("\n✓ VENDA FINALIZADA COM SUCESSO!\n");
            System.out.println(vendaGerada.gerarCupomFiscal());
        } else {
            System.out.println("[ERRO] Ocorreu uma falha durante o faturamento da venda. Nenhuma alteração foi gravada.");
        }

        return true;
    }
}
