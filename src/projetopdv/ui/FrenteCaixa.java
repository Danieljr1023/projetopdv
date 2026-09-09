package projetopdv.ui;

import java.util.Scanner;
import projetopdv.caixa.CaixaDAO;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.produto.ProdutoDAO;
import projetopdv.usuario.UsuarioDAO;
import projetopdv.venda.VendaDAO;

public class FrenteCaixa {

    public static void main(String[] args) {
        try (Scanner entrada = new Scanner(System.in)) {
            // Garante que todas as tabelas necessárias estejam criadas
            BancoDeDados.inicializarTabelas();

            // Instancia os DAOs das camadas de dados
            ProdutoDAO produtoDAO = new ProdutoDAO();
            ClienteDAO clienteDAO = new ClienteDAO();
            OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            VendaDAO vendaDAO = new VendaDAO();
            CaixaDAO caixaDAO = new CaixaDAO();
            PosVendaDAO posVendaDAO = new PosVendaDAO();

            // Inicializa a sessão dedicada do Caixa (PDV)
            SessaoCaixa sessao = new SessaoCaixa(
                    entrada,
                    vendaDAO,
                    caixaDAO,
                    posVendaDAO,
                    orcamentoDAO,
                    clienteDAO,
                    produtoDAO,
                    usuarioDAO
            );

            System.out.println("===============================================================");
            System.out.println("               SISTEMA PDV - FRENTE DE CAIXA                   ");
            System.out.println("===============================================================");
            System.out.println("Terminal dedicado para operações comerciais e financeiras de caixa:");
            System.out.println(" • Abertura e fechamento de turno com conferência cega");
            System.out.println(" • Faturamento ágil de orçamentos confirmados");
            System.out.println(" • Emissão de cupom fiscal e recibos térmicos");
            System.out.println(" • Sangrias e suprimentos de gaveta");
            System.out.println(" • Devoluções de itens com emissão de vale-compra");
            System.out.println(" • Estornos integrais com reembolso imediato");
            System.out.println("---------------------------------------------------------------");
            System.out.println("Para iniciar, faça login: /login <usuario> <senha>");
            System.out.println("Para ver a lista de comandos disponíveis, digite: /help");
            System.out.println("===============================================================");

            boolean executando = true;

            while (executando) {
                System.out.print(sessao.obterPrompt());

                if (!entrada.hasNextLine()) {
                    break;
                }

                String linha = entrada.nextLine();
                try {
                    executando = sessao.processarLinha(linha);
                } catch (Exception e) {
                    System.out.println("[ERRO INESPERADO] " + e.getMessage());
                }
            }

            System.out.println("\n[SISTEMA] Frente de Caixa finalizada. Até logo!");
        }
    }
}
