package projetopdv.ui.comandos;

import java.math.BigDecimal;
import java.util.Scanner;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoCadastrarProduto extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoCadastrarProduto(SessaoOrcamento sessao) {
        super(
                "/cadastrar_produto",
                """
                Cadastra um novo produto no sistema.
                Solicita Nome, Código de Barras e Preço de Venda.
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.PRODUTO_CADASTRAR)) {
            return true;
        }

        if (argumentos.length > 0) {
            System.out.println("Uso correto: /cadastrar_produto");
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        ProdutoDAO produtoDAO = sessao.getProdutoDAO();

        System.out.print("Insira o nome do produto: ");
        String nome = entrada.nextLine().trim();

        System.out.print("Insira o código de barras: ");
        String codigoBarras = entrada.nextLine().trim();

        BigDecimal preco = null;

        while (preco == null) {
            System.out.print("Insira o preço de venda: R$ ");
            String input = entrada.nextLine()
                    .trim()
                    .replace(",", ".");

            try {
                preco = new BigDecimal(input);
                if (preco.signum() < 0) {
                    System.out.println("O preço não pode ser negativo.");
                    preco = null;
                }
            } catch (NumberFormatException e) {
                System.out.println("Preço inválido.");
            }
        }

        try {
            Produto produto = produtoDAO.cadastrar(
                    nome,
                    codigoBarras,
                    preco
            );

            if (produto != null) {
                System.out.println("\n✅ Produto cadastrado com sucesso!");
                System.out.println(produto);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro ao cadastrar produto: " + e.getMessage());
        }

        return true;
    }
}
